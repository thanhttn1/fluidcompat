package me.thanhtnt2.fluidcompat.compat.pneumaticcraft.hopper.mixin;

import me.thanhtnt2.fluidcompat.compat.GuardedCompatMixinPlugin;

/** Applies the Liquid Hopper hook only if PneumaticCraft's hopper still has the expected {@code doImport(int)} method. */
public final class PneumaticHopperMixinPlugin extends GuardedCompatMixinPlugin {

    public PneumaticHopperMixinPlugin() {
        super("pneumaticcraft_hopper",
                "me.desht.pneumaticcraft.common.block.entity.hopper.LiquidHopperBlockEntity",
                target -> declaresMethod(target, "doImport", "(I)Z"));
    }
}
