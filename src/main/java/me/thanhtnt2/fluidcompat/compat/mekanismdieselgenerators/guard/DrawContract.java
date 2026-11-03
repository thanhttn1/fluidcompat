package me.thanhtnt2.fluidcompat.compat.mekanismdieselgenerators.guard;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * What the Mekanism Diesel Generators pump hook relies on, checked against the pump's bytecode before the hook is applied.
 *
 * <p>Needs nothing but ASM, so it can run (and be tested) without Minecraft or Mekanism Diesel Generators on the class path.
 * The hook only makes sense while {@code draw(int)} still takes water from a world source block and puts it into the tank
 * without touching the world; if any of that changes, the hook stays off and the log says why.
 */
public final class DrawContract {

    static final String DRAW_NAME = "draw";
    static final String DRAW_DESCRIPTOR = "(I)I";

    private static final String LEVEL = "net/minecraft/world/level/Level";
    private static final String FLUID_STATE = "net/minecraft/world/level/material/FluidState";
    private static final String FLUIDS = "net/minecraft/world/level/material/Fluids";
    private static final String FLUID_TANK = "net/neoforged/neoforge/fluids/capability/templates/FluidTank";
    private static final String BUCKET_PICKUP = "net/minecraft/world/level/block/BucketPickup";
    private static final String LIQUID_BLOCK_CONTAINER = "net/minecraft/world/level/block/LiquidBlockContainer";

    private DrawContract() {
    }

    /** @return the reasons the hook cannot be used, empty when everything it relies on is still there */
    public static List<String> problemsOf(ClassNode pump) {
        MethodNode draw = findDraw(pump);
        if (draw == null) {
            return List.of("the pump no longer has draw(int)");
        }
        List<String> problems = new ArrayList<>();
        int stateRead = indexOfFirstCall(draw, LEVEL, "getFluidState");
        int sourceCheck = indexOfFirstCall(draw, FLUID_STATE, "isSource");
        int firstFill = indexOfFirstCall(draw, FLUID_TANK, "fill");
        int waterConstant = indexOfFirstWaterConstantAfter(draw, stateRead);

        if (countCalls(draw, LEVEL, "getFluidState") != 1) {
            problems.add("draw(int) must read the world fluid exactly once");
        }
        if (firstFill < 0) {
            problems.add("draw(int) no longer fills its tank");
        } else if (!(stateRead >= 0 && stateRead < sourceCheck && sourceCheck < firstFill && waterConstant > 0 && waterConstant < firstFill)) {
            // The hook charges the world for the FIRST fill call, so that call must be the water-source branch
            problems.add("the first tank fill is no longer the branch that takes water from a source block");
        }
        if (removesFromWorld(draw)) {
            // MDG draining the world itself would make the hook charge the world twice
            problems.add("draw(int) now changes the world by itself");
        }
        return problems;
    }

    private static MethodNode findDraw(ClassNode pump) {
        for (MethodNode method : pump.methods) {
            if (DRAW_NAME.equals(method.name) && DRAW_DESCRIPTOR.equals(method.desc)) {
                return method;
            }
        }
        return null;
    }

    private static int indexOfFirstCall(MethodNode method, String owner, String name) {
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && owner.equals(call.owner) && name.equals(call.name)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static int countCalls(MethodNode method, String owner, String name) {
        int calls = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && owner.equals(call.owner) && name.equals(call.name)) {
                calls++;
            }
        }
        return calls;
    }

    /** Index of the first {@code Fluids.WATER} read after {@code fromIndex}, or -1. */
    private static int indexOfFirstWaterConstantAfter(MethodNode method, int fromIndex) {
        if (fromIndex < 0) {
            return -1;
        }
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (index > fromIndex
                    && instruction.getOpcode() == Opcodes.GETSTATIC
                    && instruction instanceof FieldInsnNode field
                    && FLUIDS.equals(field.owner)
                    && "WATER".equals(field.name)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static boolean removesFromWorld(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (!(instruction instanceof MethodInsnNode call)) {
                continue;
            }
            boolean levelWrite = LEVEL.equals(call.owner)
                    && (call.name.startsWith("setBlock") || call.name.startsWith("removeBlock") || call.name.startsWith("destroyBlock"));
            boolean pickup = BUCKET_PICKUP.equals(call.owner) || LIQUID_BLOCK_CONTAINER.equals(call.owner);
            if (levelWrite || pickup) {
                return true;
            }
        }
        return false;
    }
}
