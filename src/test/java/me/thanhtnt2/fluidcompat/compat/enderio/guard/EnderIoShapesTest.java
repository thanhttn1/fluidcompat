package me.thanhtnt2.fluidcompat.compat.enderio.guard;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Unit level only: the guards run on small hand-built classes with the shape of the real Ender IO 8.2.12-beta classes.
 * The real classes were checked separately (bytecode read, both guards return no problem, mutations are refused).
 */
class EnderIoShapesTest {

    private static final String LEVEL = "net/minecraft/world/level/Level";
    private static final String FLUID_STATE = "net/minecraft/world/level/material/FluidState";
    private static final String TANK = "com/enderio/enderio/foundation/io/fluid/TankAccess";
    private static final String ACTION = "net/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction";

    private static MethodInsnNode call(String owner, String name, String desc) {
        return new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, name, desc, false);
    }

    private static FieldInsnNode action(String name) {
        return new FieldInsnNode(Opcodes.GETSTATIC, ACTION, name, "L" + ACTION + ";");
    }

    private static MethodNode method(String name, String desc, List<org.objectweb.asm.tree.AbstractInsnNode> body) {
        InsnList list = new InsnList();
        body.forEach(list::add);
        list.add(new InsnNode(Opcodes.RETURN));
        MethodNode node = new MethodNode(Opcodes.ACC_PUBLIC, name, desc, null, null);
        node.instructions = list;
        return node;
    }

    private static ClassNode drain(int setBlocks, int getTypes) {
        List<org.objectweb.asm.tree.AbstractInsnNode> step = new java.util.ArrayList<>();
        step.add(call(LEVEL, "getFluidState", EnderIoShapes.GET_FLUID_STATE));
        step.add(call(FLUID_STATE, "isSource", "()Z"));
        for (int i = 0; i < getTypes; i++) {
            step.add(call(FLUID_STATE, "getType", EnderIoShapes.GET_TYPE));
        }
        step.add(action("SIMULATE"));
        step.add(call(TANK, "fill", EnderIoShapes.FILL));
        for (int i = 0; i < setBlocks; i++) {
            step.add(call(LEVEL, "setBlock", EnderIoShapes.SET_BLOCK));
        }
        step.add(action("EXECUTE"));
        step.add(call(TANK, "fill", EnderIoShapes.FILL));
        List<org.objectweb.asm.tree.AbstractInsnNode> active = List.of(
                call(LEVEL, "getFluidState", EnderIoShapes.GET_FLUID_STATE),
                call(FLUID_STATE, "isSource", "()Z"),
                call(FLUID_STATE, "getType", EnderIoShapes.GET_TYPE),
                action("SIMULATE"),
                call(TANK, "fill", EnderIoShapes.FILL));
        ClassNode node = new ClassNode();
        node.methods.add(method("drainFluids", "()V", step));
        node.methods.add(method("isActive", "()Z", active));
        return node;
    }

    private static ClassNode niard(int bucketPushes) {
        List<org.objectweb.asm.tree.AbstractInsnNode> place = List.of(
                call("net/minecraft/world/level/material/Fluid", "defaultFluidState", "()Lnet/minecraft/world/level/material/FluidState;"),
                call(LEVEL, "setBlock", EnderIoShapes.SET_BLOCK));
        List<org.objectweb.asm.tree.AbstractInsnNode> consume = new java.util.ArrayList<>();
        for (int i = 0; i < bucketPushes; i++) {
            consume.add(new IntInsnNode(Opcodes.SIPUSH, 1000));
        }
        consume.add(action("EXECUTE"));
        consume.add(call(TANK, "drain", EnderIoShapes.DRAIN));
        ClassNode node = new ClassNode();
        node.methods.add(method("placeFluid", "(Lnet/minecraft/core/BlockPos;)V", place));
        node.methods.add(method("consumeResources", "()V", consume));
        return node;
    }

    @Test
    void acceptsTheDrainShapeOfEnderIo() {
        assertNull(EnderIoShapes.drainProblem(drain(1, 3)));
    }

    @Test
    void refusesADrainWhoseCallsChanged() {
        assertTrue(EnderIoShapes.drainProblem(drain(2, 3)).contains("setBlock"));
        assertTrue(EnderIoShapes.drainProblem(drain(0, 3)).contains("setBlock"));
        assertTrue(EnderIoShapes.drainProblem(drain(1, 2)).contains("getType"));
        assertNotNull(EnderIoShapes.drainProblem(new ClassNode()));
    }

    @Test
    void refusesADrainThatAlsoRemovesBlocksItsOwnWay() {
        ClassNode node = drain(1, 3);
        node.methods.get(0).instructions.insert(call(LEVEL, "removeBlock", "(Lnet/minecraft/core/BlockPos;Z)Z"));
        assertTrue(EnderIoShapes.drainProblem(node).contains("removeBlock"));
    }

    @Test
    void acceptsTheNiardShapeOfEnderIo() {
        assertNull(EnderIoShapes.niardProblem(niard(1)));
    }

    @Test
    void refusesANiardWhoseBucketChanged() {
        assertTrue(EnderIoShapes.niardProblem(niard(0)).contains("1000"));
        assertTrue(EnderIoShapes.niardProblem(niard(2)).contains("1000"));
        assertNotNull(EnderIoShapes.niardProblem(new ClassNode()));
    }
}
