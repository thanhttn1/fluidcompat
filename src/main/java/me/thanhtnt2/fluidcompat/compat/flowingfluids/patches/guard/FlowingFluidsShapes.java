package me.thanhtnt2.fluidcompat.compat.flowingfluids.patches.guard;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Bytecode checks that say whether a Flowing Fluids method still has the exact flaw a patch repairs.
 *
 * <p>Each check answers "is the bug still there, in the shape the patch expects?". When Flowing Fluids fixes a bug
 * itself, or reshapes the method, the answer turns false and the patch switches itself off, so a fixed version is
 * never patched twice. Pure ASM and plain strings: nothing here loads Flowing Fluids or Minecraft.
 */
public final class FlowingFluidsShapes {

    private static final String NOTIFY_LISTENERS = "notifyListeners";
    private static final String NOTIFY_LISTENERS_DESC =
            "(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/material/Fluid;Z)V";
    private static final String GET_BLOCK_ENTITY = "getBlockEntity";
    private static final String GET_BLOCK_ENTITY_DESC =
            "(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;";
    private static final String ACCEPT_RECENT_FLOW = "ff$acceptRecentFlow";
    private static final String ACCEPT_RECENT_FLOW_DESC =
            "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/material/Fluid;Z)V";

    private static final String REMOVE_FLUID = "removeFluidAmountFromPos";
    private static final String REMOVE_FLUID_DESC =
            "(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/level/material/Fluid;II)I";
    private static final String COLLECT_AND_REMOVE = "collectConnectedFluidAmountAndRemove";
    // Local variable slots of the instance method removeFluidAmountFromPos: this, level, pos, fluid, min, max
    private static final int MAX_AMOUNT_SLOT = 5;
    // Local variable slot of the fluid position in the static method notifyListeners: level, fluidPos, ...
    private static final int FLUID_POS_SLOT = 1;

    private FlowingFluidsShapes() {
    }

    /**
     * True while {@code FFFluidUtils.notifyListeners} asks for the block entity at the position of the fluid cell
     * (local variable 1) instead of at the position of the listener it found in the map, and still hands the result
     * to {@code IFFFlowListener.ff$acceptRecentFlow}.
     */
    public static boolean flowListenerLookupIsBroken(ClassNode fluidUtils) {
        MethodNode method = find(fluidUtils, NOTIFY_LISTENERS, NOTIFY_LISTENERS_DESC);
        if (method == null) {
            return false;
        }
        int blockEntityLookups = 0;
        boolean lookupUsesFluidPos = false;
        boolean callsListener = false;
        AbstractInsnNode previous = null;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() < 0) {
                continue; // labels, line numbers and frames are not real instructions
            }
            if (instruction instanceof MethodInsnNode call) {
                if (GET_BLOCK_ENTITY.equals(call.name) && GET_BLOCK_ENTITY_DESC.equals(call.desc)) {
                    blockEntityLookups++;
                    lookupUsesFluidPos = previous instanceof VarInsnNode load
                            && load.getOpcode() == Opcodes.ALOAD && load.var == FLUID_POS_SLOT;
                }
                if (ACCEPT_RECENT_FLOW.equals(call.name) && ACCEPT_RECENT_FLOW_DESC.equals(call.desc)) {
                    callsListener = true;
                }
            }
            previous = instruction;
        }
        return blockEntityLookups == 1 && lookupUsesFluidPos && callsListener;
    }

    /**
     * True while {@code FlowingFluidsApiImpl.removeFluidAmountFromPos} still hands {@code maxAmount} to the collector
     * without ever comparing it with zero.
     */
    public static boolean removalAcceptsNegativeAmounts(ClassNode apiImplementation) {
        MethodNode method = find(apiImplementation, REMOVE_FLUID, REMOVE_FLUID_DESC);
        if (method == null) {
            return false;
        }
        boolean reachesCollector = false;
        AbstractInsnNode[] instructions = method.instructions.toArray();
        for (int index = 0; index < instructions.length; index++) {
            AbstractInsnNode instruction = instructions[index];
            if (instruction instanceof MethodInsnNode call && COLLECT_AND_REMOVE.equals(call.name)) {
                reachesCollector = true;
            }
            if (instruction instanceof VarInsnNode load && load.getOpcode() == Opcodes.ILOAD
                    && load.var == MAX_AMOUNT_SLOT && isZeroComparison(nextRealInstruction(instructions, index))) {
                return false; // Flowing Fluids validates the amount itself now
            }
        }
        return reachesCollector;
    }

    private static boolean isZeroComparison(AbstractInsnNode instruction) {
        if (!(instruction instanceof JumpInsnNode jump)) {
            return false;
        }
        int opcode = jump.getOpcode();
        return opcode == Opcodes.IFLT || opcode == Opcodes.IFLE || opcode == Opcodes.IFGT || opcode == Opcodes.IFGE;
    }

    private static AbstractInsnNode nextRealInstruction(AbstractInsnNode[] instructions, int index) {
        for (int next = index + 1; next < instructions.length; next++) {
            if (instructions[next].getOpcode() >= 0) {
                return instructions[next];
            }
        }
        return null;
    }

    private static MethodNode find(ClassNode classNode, String name, String descriptor) {
        for (MethodNode method : classNode.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) {
                return method;
            }
        }
        return null;
    }
}
