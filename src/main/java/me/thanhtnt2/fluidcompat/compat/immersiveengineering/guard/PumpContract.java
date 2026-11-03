package me.thanhtnt2.fluidcompat.compat.immersiveengineering.guard;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Everything Fluid Compat assumes about how the Immersive Engineering Fluid Pump works, written down as checks on
 * its bytecode. It is the single place that decides whether the Immersive Engineering compat may run.
 *
 * <p>The compat changes how the pump treats finite fluids in four places that depend on each other: it stops the
 * endless-source rule, lets the area search see thin water layers, lets the pump take them, and keeps the pump from
 * replacing emptied blocks with cobblestone. Running only some of these would leave the pump half changed, so the
 * contract is all or nothing: if any check fails, none of the Immersive Engineering hooks are applied.
 *
 * <p>This class uses only ASM and the JDK, so it can be run against any Immersive Engineering jar without Minecraft.
 */
public final class PumpContract {

    private static final String UTILS = "blusunrize/immersiveengineering/common/util/Utils";
    private static final String LEVEL = "net/minecraft/world/level/Level";
    private static final String FLUID_STATE = "net/minecraft/world/level/material/FluidState";
    private static final String BLOCKS = "net/minecraft/world/level/block/Blocks";
    private static final String FLUID_ACTION = "net/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction";
    private static final String BUCKET_PICKUP = "net/minecraft/world/level/block/BucketPickup";

    private static final String DRAIN_DESC = "(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;"
            + "Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/neoforged/neoforge/fluids/FluidStack;";
    private static final String RELATED_DESC = "(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)"
            + "Lnet/minecraft/world/level/material/Fluid;";
    private static final String CONVERT_DESC = "(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z";
    private static final String SET_BLOCK_DESC = "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z";
    private static final String GET_TYPE_DESC = "()Lnet/minecraft/world/level/material/Fluid;";

    /** The result of a check: either everything matches, or a list of what does not. */
    public record Verdict(boolean ok, List<String> problems) {

        public static Verdict notInstalled() {
            return new Verdict(false, List.of("Immersive Engineering is not installed"));
        }

        public String describe() {
            return ok ? "all checks passed" : String.join("; ", problems);
        }
    }

    private PumpContract() {
    }

    public static Verdict verify(ClassNode pump, ClassNode utils) {
        List<String> problems = new ArrayList<>();
        checkPump(pump, problems);
        checkUtils(utils, problems);
        return new Verdict(problems.isEmpty(), List.copyOf(problems));
    }

    private static void checkPump(ClassNode pump, List<String> problems) {
        requireField(pump, "closedList", "Ljava/util/List;", problems);
        requireField(pump, "placeCobble", "Z", problems);
        requireField(pump, "searchFluid", "Lnet/minecraft/world/level/material/Fluid;", problems);

        MethodNode tick = findMethod(pump, "tickServer", "()V");
        MethodNode area = findMethod(pump, "checkAreaTick", "()V");
        MethodNode gather = findMethod(pump, "gatherInfiniteFluidFromWorld", "(Lnet/minecraft/core/Direction;)V");
        if (tick == null || area == null || gather == null) {
            problems.add("pump is missing tickServer, checkAreaTick or gatherInfiniteFluidFromWorld");
            return;
        }

        // The endless-source rule is asked exactly where the hook answers it, and nowhere else in the class
        expect(problems, "canConvertToSource in gatherInfiniteFluidFromWorld", countCalls(gather, FLUID_STATE, "canConvertToSource", CONVERT_DESC), 1);
        expect(problems, "canConvertToSource in checkAreaTick", countCalls(area, FLUID_STATE, "canConvertToSource", CONVERT_DESC), 2);
        int convertInWholeClass = 0;
        for (MethodNode method : pump.methods) {
            convertInWholeClass += countCalls(method, FLUID_STATE, "canConvertToSource", CONVERT_DESC);
        }
        expect(problems, "canConvertToSource in the whole pump class", convertInWholeClass, 3);

        // The area search compares fluids by identity: one place reads the type, one asks Utils for the related fluid
        expect(problems, "FluidState.getType in checkAreaTick", countCalls(area, FLUID_STATE, "getType", GET_TYPE_DESC), 1);
        expect(problems, "Utils.getRelatedFluid in checkAreaTick", countCalls(area, UTILS, "getRelatedFluid", RELATED_DESC), 1);
        expect(problems, "Utils.drainFluidBlock in checkAreaTick", countCalls(area, UTILS, "drainFluidBlock", DRAIN_DESC), 1);
        // The cobblestone hook reads the fluid the search found, so the search must still write it
        if (!writesField(area, "searchFluid")) {
            problems.add("checkAreaTick no longer stores the fluid it searches for in searchFluid");
        }

        checkTick(tick, problems);
    }

    private static void checkTick(MethodNode tick, List<String> problems) {
        expect(problems, "Utils.drainFluidBlock in tickServer", countCalls(tick, UTILS, "drainFluidBlock", DRAIN_DESC), 2);
        expect(problems, "Level.setBlockAndUpdate in tickServer", countCalls(tick, LEVEL, "setBlockAndUpdate", SET_BLOCK_DESC), 1);
        expect(problems, "List.size in tickServer (work list end)", countCalls(tick, "java/util/List", "size", "()I") > 0 ? 1 : 0, 1);
        expect(problems, "List.get in tickServer (work list end)", countCalls(tick, "java/util/List", "get", "(I)Ljava/lang/Object;") > 0 ? 1 : 0, 1);

        int secondDrain = indexOfCall(tick, UTILS, "drainFluidBlock", 1);
        int setBlock = indexOfCall(tick, LEVEL, "setBlockAndUpdate", 0);
        int cobbleRead = indexOfField(tick, BLOCKS, "COBBLESTONE");
        int flagRead = indexOfField(tick, null, "placeCobble");
        if (secondDrain < 0 || setBlock < 0 || cobbleRead < 0 || flagRead < 0) {
            problems.add("tickServer no longer reads placeCobble and Blocks.COBBLESTONE around its block placement");
        } else if (!(secondDrain < flagRead && flagRead < cobbleRead && cobbleRead < setBlock)) {
            problems.add("tickServer no longer takes the block first and then checks placeCobble before placing cobblestone");
        }
    }

    private static void checkUtils(ClassNode utils, List<String> problems) {
        MethodNode drain = findMethod(utils, "drainFluidBlock", DRAIN_DESC);
        MethodNode related = findMethod(utils, "getRelatedFluid", RELATED_DESC);
        if (drain == null || related == null) {
            problems.add("Utils is missing drainFluidBlock or getRelatedFluid");
            return;
        }
        if ((drain.access & Opcodes.ACC_STATIC) == 0 || (related.access & Opcodes.ACC_STATIC) == 0) {
            problems.add("Utils.drainFluidBlock or getRelatedFluid is no longer static");
        }

        // getRelatedFluid must stay "level.getFluidState(pos).getType()": the hook re-implements exactly that
        expect(problems, "calls inside Utils.getRelatedFluid", countAllCalls(related), 2);
        expect(problems, "FluidState.getType in Utils.getRelatedFluid", countCalls(related, FLUID_STATE, "getType", GET_TYPE_DESC), 1);

        // drainFluidBlock must stay "source block -> pick it up -> 1000 mB", with the pickup only when executing
        expect(problems, "FluidState.isSource in Utils.drainFluidBlock", countCalls(drain, FLUID_STATE, "isSource", "()Z"), 1);
        expect(problems, "BucketPickup.pickupBlock in Utils.drainFluidBlock",
                countCallsByName(drain, BUCKET_PICKUP, "pickupBlock"), 1);
        expect(problems, "FluidAction.execute in Utils.drainFluidBlock", countCalls(drain, FLUID_ACTION, "execute", "()Z"), 1);
        if (!hasInstanceOf(drain, BUCKET_PICKUP)) {
            problems.add("Utils.drainFluidBlock no longer tests for BucketPickup");
        }
        if (!hasIntConstant(drain, 1000)) {
            problems.add("Utils.drainFluidBlock no longer returns 1000 mB per block");
        }
    }

    private static void expect(List<String> problems, String what, int actual, int expected) {
        if (actual != expected) {
            problems.add(what + ": found " + actual + ", expected " + expected);
        }
    }

    private static void requireField(ClassNode owner, String name, String descriptor, List<String> problems) {
        boolean present = owner.fields.stream().anyMatch(field -> name.equals(field.name) && descriptor.equals(field.desc));
        if (!present) {
            problems.add("pump field " + name + " " + descriptor + " not found");
        }
    }

    private static MethodNode findMethod(ClassNode owner, String name, String descriptor) {
        for (MethodNode method : owner.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) {
                return method;
            }
        }
        return null;
    }

    private static int countCalls(MethodNode method, String owner, String name, String descriptor) {
        int calls = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && name.equals(call.name) && descriptor.equals(call.desc) && owner.equals(call.owner)) {
                calls++;
            }
        }
        return calls;
    }

    private static int countCallsByName(MethodNode method, String owner, String name) {
        int calls = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && name.equals(call.name) && owner.equals(call.owner)) {
                calls++;
            }
        }
        return calls;
    }

    private static int countAllCalls(MethodNode method) {
        int calls = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode) {
                calls++;
            }
        }
        return calls;
    }

    /** Position in the instruction list of the n-th (0-based) matching call, or -1. */
    private static int indexOfCall(MethodNode method, String owner, String name, int nth) {
        int seen = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && name.equals(call.name) && owner.equals(call.owner)) {
                if (seen++ == nth) {
                    return method.instructions.indexOf(instruction);
                }
            }
        }
        return -1;
    }

    /** Position of the first read of a field (any owner when {@code owner} is null), or -1. */
    private static int indexOfField(MethodNode method, String owner, String name) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field && name.equals(field.name) && (owner == null || owner.equals(field.owner))) {
                return method.instructions.indexOf(instruction);
            }
        }
        return -1;
    }

    private static boolean writesField(MethodNode method, String name) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field && field.getOpcode() == Opcodes.PUTFIELD && name.equals(field.name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasInstanceOf(MethodNode method, String type) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof TypeInsnNode node && node.getOpcode() == Opcodes.INSTANCEOF && type.equals(node.desc)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasIntConstant(MethodNode method, int value) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof IntInsnNode node && node.operand == value
                    && (node.getOpcode() == Opcodes.SIPUSH || node.getOpcode() == Opcodes.BIPUSH)) {
                return true;
            }
        }
        return false;
    }
}
