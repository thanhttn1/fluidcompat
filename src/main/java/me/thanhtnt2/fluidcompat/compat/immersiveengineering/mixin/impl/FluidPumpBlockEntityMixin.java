package me.thanhtnt2.fluidcompat.compat.immersiveengineering.mixin.impl;

import me.thanhtnt2.fluidcompat.compat.immersiveengineering.FinitePumpRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The only place Fluid Compat touches the Immersive Engineering Fluid Pump. The target is referenced by name so
 * Fluid Compat loads without Immersive Engineering; {@code ImmersiveEngineeringMixinPlugin} decides whether this
 * applies at all.
 */
@Mixin(targets = "blusunrize.immersiveengineering.common.blocks.metal.FluidPumpBlockEntity", remap = false)
public class FluidPumpBlockEntityMixin {

    // Three calls in two methods: the endless-gather check and the two checks of the area search
    @Redirect(
            method = {"gatherInfiniteFluidFromWorld(Lnet/minecraft/core/Direction;)V", "checkAreaTick()V"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/material/FluidState;canConvertToSource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z",
                    remap = false),
            remap = false)
    private boolean fluidcompat$finiteFluidIsNoEndlessSource(FluidState fluidState, Level level, BlockPos pos) {
        return FinitePumpRules.mayConvertToSource(fluidState, level, pos);
    }
}
