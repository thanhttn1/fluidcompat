package me.thanhtnt2.fluidcompat.compat.modernindustrialization.mixin.impl;

import me.thanhtnt2.fluidcompat.compat.modernindustrialization.MiPumpBridge;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets the water pump see the thin, non-source water layers Flowing Fluids leaves behind.
 *
 * <p>Modern Industrialization counts only source blocks around the pump, so a pond that has been drained down
 * to thin layers would keep the pump running without ever giving water again. Counting such a layer as one
 * water cell makes the pump finish the pond. The water is still paid by the world in {@code MiPumpBridge},
 * so the pump can never give more than the world holds.
 *
 * <p>Kept apart from the main pump mixin on purpose: both injectors use {@code require = 0}, so if Modern
 * Industrialization ever changes this method, only this feature switches itself off and the hook that stops
 * the pump from creating water stays active.
 */
@Mixin(targets = "aztech.modern_industrialization.machines.blockentities.AbstractWaterPumpBlockEntity", remap = false)
public class AbstractWaterPumpThinWaterMixin {

    // First of the two checks MI makes on every cell around the pump: "is it a source?"
    @Redirect(
            method = "getWaterSourceCount()I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;isSource()Z"),
            require = 0,
            remap = false)
    private boolean fluidcompat$countThinWaterAsSource(FluidState fluidState) {
        return fluidState.isSource() || MiPumpBridge.isThinPumpableWater(fluidState);
    }

    // Second check: "is it plain water?" MI compares with the still-water instance, flowing water would fail it
    @Redirect(
            method = "getWaterSourceCount()I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getType()Lnet/minecraft/world/level/material/Fluid;"),
            require = 0,
            remap = false)
    private Fluid fluidcompat$normalizeThinWaterType(FluidState fluidState) {
        return MiPumpBridge.isThinPumpableWater(fluidState) ? Fluids.WATER : fluidState.getType();
    }
}
