package me.thanhtnt2.fluidcompat.compat.enderio.guard;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bytecode checks for the Ender IO hooks. Each hook steers several calls inside one method, so each check says "this
 * method still has exactly these calls, in this order" and nothing else that could change the world. Anything else and
 * the hook stays off with a reason in the log. Pure ASM and pure strings: nothing here loads Minecraft.
 *
 * <p>Checked against Ender IO 8.2.12-beta (the real classes pass, hand-made mutations do not).
 */
public final class EnderIoShapes {

    private static final Logger LOGGER = LoggerFactory.getLogger("FluidCompat/Mixin");

    static final String LEVEL = "net/minecraft/world/level/Level";
    static final String FLUID_STATE = "net/minecraft/world/level/material/FluidState";
    static final String FLUID = "net/minecraft/world/level/material/Fluid";
    static final String TANK = "com/enderio/enderio/foundation/io/fluid/TankAccess";
    static final String FLUID_ACTION = "net/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction";

    static final String GET_FLUID_STATE = "(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;";
    static final String SET_BLOCK = "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z";
    static final String GET_TYPE = "()Lnet/minecraft/world/level/material/Fluid;";
    static final String FILL = "(Lcom/enderio/enderio/foundation/attachment/FluidTankUser;"
            + "Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I";
    static final String DRAIN = "(Lcom/enderio/enderio/foundation/attachment/FluidTankUser;I"
            + "Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/neoforged/neoforge/fluids/FluidStack;";

    private EnderIoShapes() {
    }

    // ------------------------------------------------------------------ Drain

    public static boolean isDrainHookable(ClassNode drain) {
        return report("enderio_drain", drainProblem(drain));
    }

    /** @return what is missing, in words for the log, or null when both Drain methods have the expected shape */
    public static String drainProblem(ClassNode drain) {
        MethodNode step = find(drain, "drainFluids", "()V");
        MethodNode active = find(drain, "isActive", "()Z");
        if (step == null) {
            return "no drainFluids()V";
        }
        if (active == null) {
            return "no isActive()Z";
        }
        String problem = firstProblem(
                expectCalls(step, "drainFluids", LEVEL, "getFluidState", GET_FLUID_STATE, 1),
                expectCalls(step, "drainFluids", LEVEL, "setBlock", SET_BLOCK, 1),
                expectCalls(step, "drainFluids", FLUID_STATE, "isSource", "()Z", 1),
                expectCalls(step, "drainFluids", FLUID_STATE, "getType", GET_TYPE, 3),
                expectCalls(step, "drainFluids", TANK, "fill", FILL, 2),
                expectOnlyLevelCalls(step, "drainFluids"),
                expectActions(step, "drainFluids", "SIMULATE", "EXECUTE"),
                expectCalls(active, "isActive", LEVEL, "getFluidState", GET_FLUID_STATE, 1),
                expectCalls(active, "isActive", FLUID_STATE, "isSource", "()Z", 1),
                expectCalls(active, "isActive", FLUID_STATE, "getType", GET_TYPE, 1),
                expectCalls(active, "isActive", TANK, "fill", FILL, 1),
                expectOnlyLevelCalls(active, "isActive"),
                expectActions(active, "isActive", "SIMULATE"));
        return problem;
    }

    // ------------------------------------------------------------------ Niard

    public static boolean isNiardHookable(ClassNode niard) {
        return report("enderio_niard", niardProblem(niard));
    }

    /** @return what is missing, in words for the log, or null when both Niard methods have the expected shape */
    public static String niardProblem(ClassNode niard) {
        MethodNode place = find(niard, "placeFluid", "(Lnet/minecraft/core/BlockPos;)V");
        MethodNode consume = find(niard, "consumeResources", "()V");
        if (place == null) {
            return "no placeFluid(BlockPos)V";
        }
        if (consume == null) {
            return "no consumeResources()V";
        }
        return firstProblem(
                expectCalls(place, "placeFluid", FLUID, "defaultFluidState", "()Lnet/minecraft/world/level/material/FluidState;", 1),
                expectCalls(place, "placeFluid", LEVEL, "setBlock", SET_BLOCK, 1),
                expectOnlyLevelCalls(place, "placeFluid"),
                expectCalls(consume, "consumeResources", TANK, "drain", DRAIN, 1),
                expectActions(consume, "consumeResources", "EXECUTE"),
                expectBucketConstant(consume, "consumeResources"));
    }

    // ------------------------------------------------------------------ helpers

    private static boolean report(String id, String problem) {
        if (problem != null) {
            LOGGER.warn("[{}] guard refused the class: {}", id, problem);
        }
        return problem == null;
    }

    private static String firstProblem(String... problems) {
        for (String problem : problems) {
            if (problem != null) {
                return problem;
            }
        }
        return null;
    }

    private static MethodNode find(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) {
                return method;
            }
        }
        return null;
    }

    private static String expectCalls(MethodNode method, String label, String owner, String name, String descriptor, int expected) {
        int found = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && owner.equals(call.owner)
                    && name.equals(call.name) && descriptor.equals(call.desc)) {
                found++;
            }
        }
        if (found != expected) {
            return label + " calls " + owner.substring(owner.lastIndexOf('/') + 1) + "." + name + " " + found + " times, not " + expected;
        }
        return null;
    }

    // Anything else that could touch the world (removeBlock, destroyBlock, another setBlock ...) would be skipped or doubled
    private static String expectOnlyLevelCalls(MethodNode method, String label) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && LEVEL.equals(call.owner)
                    && !("getFluidState".equals(call.name) || "setBlock".equals(call.name))) {
                return label + " also calls Level." + call.name;
            }
        }
        return null;
    }

    // The order of the FluidAction constants tells which fill/drain is the simulation and which one really moves fluid
    private static String expectActions(MethodNode method, String label, String... expectedOrder) {
        List<String> found = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field && field.getOpcode() == Opcodes.GETSTATIC
                    && FLUID_ACTION.equals(field.owner)) {
                found.add(field.name);
            }
        }
        if (!found.equals(List.of(expectedOrder))) {
            return label + " reads the fluid actions " + found + ", not " + List.of(expectedOrder);
        }
        return null;
    }

    private static String expectBucketConstant(MethodNode method, String label) {
        int buckets = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof IntInsnNode push && push.getOpcode() == Opcodes.SIPUSH && push.operand == 1000) {
                buckets++;
            }
        }
        return buckets == 1 ? null : label + " pushes the 1000 mB bucket " + buckets + " times, not once";
    }
}
