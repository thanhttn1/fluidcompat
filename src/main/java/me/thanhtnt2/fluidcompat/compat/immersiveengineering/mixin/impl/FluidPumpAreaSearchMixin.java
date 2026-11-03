package me.thanhtnt2.fluidcompat.compat.immersiveengineering.mixin.impl;

import me.thanhtnt2.fluidcompat.compat.immersiveengineering.ThinFluidDrain;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes the pump's area search walk through thin layers of a finite fluid.
 *
 * <p>The search compares fluids with {@code ==} against the first fluid it found, so a thin layer ("flowing water")
 * never matched a source block ("water"). Both places that read a cell's fluid are changed to report the source
 * variant for thin layers of finite fluids; for everything else they answer exactly as before.
 */
@Mixin(targets = "blusunrize.immersiveengineering.common.blocks.metal.FluidPumpBlockEntity", remap = false)
public class FluidPumpAreaSearchMixin {

    // The one FluidState.getType call in checkAreaTick: the cell the search is looking at
    @Redirect(
            method = "checkAreaTick()V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/material/FluidState;getType()Lnet/minecraft/world/level/material/Fluid;",
                    remap = false),
            remap = false)
    private Fluid fluidcompat$thinFluidCountsAsSourceFluid(FluidState fluidState) {
        return ThinFluidDrain.normalizedType(fluidState);
    }

    // The one Utils.getRelatedFluid call: the neighbour the search may step onto. That helper is just
    // level.getFluidState(pos).getType(), which is exactly what is reproduced here (the guard checks it)
    @Redirect(
            method = "checkAreaTick()V",
            at = @At(value = "INVOKE",
                    target = "Lblusunrize/immersiveengineering/common/util/Utils;getRelatedFluid(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/Fluid;",
                    remap = false),
            remap = false)
    private Fluid fluidcompat$neighbourThinFluidCountsAsSourceFluid(Level level, BlockPos pos) {
        return ThinFluidDrain.normalizedType(level.getFluidState(pos));
    }
}
