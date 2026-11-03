package me.thanhtnt2.fluidcompat.compat.alexscaves.mixin.impl;

import me.thanhtnt2.fluidcompat.compat.alexscaves.AlexsCavesDrainBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The only place Fluid Compat touches the Drain. The target is referenced by name so Fluid Compat loads
 * without Alex's Caves; {@code AlexsCavesMixinPlugin} decides whether this applies at all.
 */
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.block.DrainBlock", remap = false)
public class DrainBlockMixin {

    // The bridge answers false whenever the original logic should run (fluid not finite, feature off, any error)
    @Inject(
            method = "drainLogic(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void fluidcompat$takeOverFiniteDrain(BlockState state, Level level, BlockPos pos, CallbackInfo callback) {
        if (AlexsCavesDrainBridge.tryHandle(state, level, pos)) {
            callback.cancel();
        }
    }
}
