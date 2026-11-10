package me.thanhtnt2.fluidcompat.compat.mekanismelements.guard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Unit level only: the guard is run on small hand-built classes that mimic the shape of the real pump. The real
 * Mekanism Elements 3.0.19 class was checked separately (bytecode read, guard returns no problem).
 */
class SeawaterWaterCheckShapeTest {

    private static final String FLUID_STATE = "net/minecraft/world/level/material/FluidState";
    private static final String FLUIDS = "net/minecraft/world/level/material/Fluids";
    private static final String GET_TYPE_DESC = "()Lnet/minecraft/world/level/material/Fluid;";

    private static ClassNode pump(String updateName, String updateDesc, int isSourceCalls, int getTypeCalls, int waterReads) {
        InsnList code = new InsnList();
        for (int i = 0; i < isSourceCalls; i++) {
            code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, FLUID_STATE, "isSource", "()Z", false));
        }
        for (int i = 0; i < getTypeCalls; i++) {
            code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, FLUID_STATE, "getType", GET_TYPE_DESC, false));
        }
        for (int i = 0; i < waterReads; i++) {
            code.add(new FieldInsnNode(Opcodes.GETSTATIC, FLUIDS, "WATER", "Lnet/minecraft/world/level/material/FlowingFluid;"));
        }
        code.add(new InsnNode(Opcodes.RETURN));
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, updateName, updateDesc, null, null);
        method.instructions = code;
        ClassNode node = new ClassNode();
        node.methods.add(method);
        return node;
    }

    @Test
    void acceptsTheExpectedShape() {
        assertNull(SeawaterWaterCheckShape.problem(pump("onUpdateServer", "()Z", 1, 1, 1)));
    }

    @Test
    void refusesARenamedOrReshapedUpdateMethod() {
        assertNotNull(SeawaterWaterCheckShape.problem(pump("onUpdate", "()Z", 1, 1, 1)));
        assertNotNull(SeawaterWaterCheckShape.problem(pump("onUpdateServer", "()V", 1, 1, 1)));
    }

    @Test
    void refusesWhenAQuestionIsAskedZeroOrTwoTimes() {
        assertTrue(SeawaterWaterCheckShape.problem(pump("onUpdateServer", "()Z", 0, 1, 1)).contains("isSource"));
        assertTrue(SeawaterWaterCheckShape.problem(pump("onUpdateServer", "()Z", 2, 1, 1)).contains("isSource"));
        assertTrue(SeawaterWaterCheckShape.problem(pump("onUpdateServer", "()Z", 1, 2, 1)).contains("getType"));
        assertTrue(SeawaterWaterCheckShape.problem(pump("onUpdateServer", "()Z", 1, 1, 0)).contains("Fluids.WATER"));
    }

    @Test
    void ignoresCallsOnOtherOwners() {
        ClassNode node = pump("onUpdateServer", "()Z", 1, 1, 1);
        node.methods.get(0).instructions.insert(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "other/Owner", "isSource", "()Z", false));
        assertEquals(null, SeawaterWaterCheckShape.problem(node));
    }
}
