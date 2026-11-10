package me.thanhtnt2.fluidcompat.compat.mekanismelements.mixin.impl;

import me.thanhtnt2.fluidcompat.compat.mekanismelements.SeawaterThinWater;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes the Seawater Pump accept thin Flowing Fluids water below it. Its own mixin and config on purpose: this hook
 * and the one that charges the world for the seawater apply or fail independently, and neither can stop the game.
 * NeoForge 1.21.1 runs with Mojang names, so nothing here needs remapping.
 */
@Mixin(targets = "fixdol.mekanismelements.common.tile.machine.TileEntitySeawaterPump", remap = false)
public class TileEntitySeawaterPumpThinWaterMixin {

    @Redirect(
            method = "onUpdateServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/material/FluidState;isSource()Z",
                    ordinal = 0,
                    remap = false),
            remap = false,
            require = 0)
    private boolean fluidcompat$isSourceForPump(FluidState state) {
        return SeawaterThinWater.isSourceForPump(state);
    }

    @Redirect(
            method = "onUpdateServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/material/FluidState;getType()Lnet/minecraft/world/level/material/Fluid;",
                    ordinal = 0,
                    remap = false),
            remap = false,
            require = 0)
    private Fluid fluidcompat$fluidForPump(FluidState state) {
        return SeawaterThinWater.fluidForPump(state);
    }
}
