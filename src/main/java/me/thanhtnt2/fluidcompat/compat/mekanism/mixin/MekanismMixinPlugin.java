package me.thanhtnt2.fluidcompat.compat.mekanism.mixin;

import me.thanhtnt2.fluidcompat.compat.GuardedCompatMixinPlugin;

/** Applies the Electric Pump hook only if Mekanism's pump still has the {@code shouldPump} decision method. */
public final class MekanismMixinPlugin extends GuardedCompatMixinPlugin {

    public MekanismMixinPlugin() {
        super("mekanism",
                "mekanism.common.tile.machine.TileEntityElectricPump",
                target -> declaresMethod(target, "shouldPump",
                        "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/material/Fluid;)Z"));
    }
}
