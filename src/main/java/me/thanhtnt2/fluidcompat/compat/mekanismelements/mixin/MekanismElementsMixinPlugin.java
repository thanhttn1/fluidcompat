package me.thanhtnt2.fluidcompat.compat.mekanismelements.mixin;

import me.thanhtnt2.fluidcompat.compat.GuardedCompatMixinPlugin;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Applies the Seawater Pump hook only if its update method still inserts into a Mekanism tank. */
public final class MekanismElementsMixinPlugin extends GuardedCompatMixinPlugin {

    private static final String TANK_INTERNAL_NAME = "mekanism/common/capabilities/fluid/BasicFluidTank";

    public MekanismElementsMixinPlugin() {
        super("mekanism_elements",
                "fixdol.mekanismelements.common.tile.machine.TileEntitySeawaterPump",
                MekanismElementsMixinPlugin::updateInsertsIntoTank);
    }

    private static boolean updateInsertsIntoTank(ClassNode target) {
        for (MethodNode method : target.methods) {
            if ("onUpdateServer".equals(method.name) && "()Z".equals(method.desc) && callsTankInsert(method)) {
                return true;
            }
        }
        return false;
    }

    private static boolean callsTankInsert(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && "insert".equals(call.name)
                    && TANK_INTERNAL_NAME.equals(call.owner)) {
                return true;
            }
        }
        return false;
    }
}
