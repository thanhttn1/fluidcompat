package me.thanhtnt2.fluidcompat.compat.immersiveengineering.mixin.impl;

import me.thanhtnt2.fluidcompat.compat.immersiveengineering.ThinFluidDrain;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets {@code Utils.drainFluidBlock}, which only the Fluid Pump calls, take a thin layer of a finite fluid.
 * Anything this does not recognise is left to Immersive Engineering's own code.
 */
@Mixin(targets = "blusunrize.immersiveengineering.common.util.Utils", remap = false)
public class UtilsDrainFluidBlockMixin {

    @Inject(
            method = "drainFluidBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/neoforged/neoforge/fluids/FluidStack;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private static void fluidcompat$takeThinFiniteFluid(
            Level level, BlockPos pos, IFluidHandler.FluidAction action, CallbackInfoReturnable<FluidStack> callback) {
        FluidStack thinLayer = ThinFluidDrain.tryDrain(level, pos, action);
        if (thinLayer != null) {
            callback.setReturnValue(thinLayer);
        }
    }
}
