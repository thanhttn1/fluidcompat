package me.thanhtnt2.fluidcompat.compat.flowingfluids.patches.mixin.impl;

import me.thanhtnt2.fluidcompat.compat.flowingfluids.patches.PatchHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Performance patch (Flowing Fluids issues 153 and 192): skips NeoForge's fluid interaction check when a liquid block
 * only changed its level.
 *
 * <p>In both methods the vanilla/NeoForge body is "if no interaction happened, schedule a fluid tick". The tick is
 * what keeps fluid flowing, so it must never be skipped. For a pure level change this handler therefore schedules
 * the tick itself, exactly as the original does, and only leaves out the interaction check in front of it. Everything
 * else runs the original method. {@code FlowingFluidsPatchesMixinPlugin} lets this mixin in only when the two methods
 * still have that exact shape.
 */
@Mixin(targets = "net.minecraft.world.level.block.LiquidBlock", remap = false)
public abstract class LiquidBlockInteractionShortcutMixin {

    @Shadow
    @Final
    private FlowingFluid fluid;

    @Inject(
            method = "neighborChanged(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;Z)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void fluidcompat$skipInteractionCheckOnLevelChange(
            BlockState state, Level level, BlockPos pos, Block neighbourBefore, BlockPos neighbourPos, boolean movedByPiston, CallbackInfo callback) {
        if (PatchHooks.canSkipInteractionCheckForNeighbour(state, level, neighbourBefore, neighbourPos)) {
            scheduleFluidTick(state, level, pos);
            callback.cancel();
        }
    }

    @Inject(
            method = "onPlace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void fluidcompat$skipInteractionCheckOnLevelPlacement(
            BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo callback) {
        if (PatchHooks.canSkipInteractionCheckForPlacement(state, oldState)) {
            scheduleFluidTick(state, level, pos);
            callback.cancel();
        }
    }

    // The same call the original makes when no interaction took place
    private void scheduleFluidTick(BlockState state, Level level, BlockPos pos) {
        level.scheduleTick(pos, state.getFluidState().getType(), this.fluid.getTickDelay(level));
    }
}
