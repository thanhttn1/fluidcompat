package me.thanhtnt2.fluidcompat.compat.mekanism.mixin.impl;

import me.thanhtnt2.fluidcompat.compat.mekanism.MekanismPumpBridge;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The only place Fluid Compat touches the Mekanism Electric Pump. Target is referenced by name so
 * Fluid Compat loads without Mekanism; {@code MekanismMixinPlugin} decides whether this applies.
 */
@Mixin(targets = "mekanism.common.tile.machine.TileEntityElectricPump", remap = false)
public class TileEntityElectricPumpMixin {

    // Mekanism skips source removal for "infinite" fluids; answering true here makes it remove them like any other
    @Inject(method = "shouldPump", at = @At("HEAD"), cancellable = true, remap = false)
    private void fluidcompat$forceSourceRemoval(Level level, Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        if (MekanismPumpBridge.shouldForceSourceRemoval(level, fluid)) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }
}
