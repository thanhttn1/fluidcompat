package me.thanhtnt2.fluidcompat.compat.immersiveengineering.mixin.impl;

import java.util.List;

import me.thanhtnt2.fluidcompat.compat.immersiveengineering.FinitePumpRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the Fluid Pump from putting cobblestone where it emptied a block of a finite fluid.
 *
 * <p>After taking a block the pump replaces it with cobblestone, and by then the block is already air, so the fluid
 * can no longer be looked up. The fluid is therefore checked just before the pump takes the block, and the result is
 * kept until the cobblestone call that follows in the same tick. The pump also places cobblestone on a block that
 * turned out to be empty already (its work list is older than the water, which Flowing Fluids keeps moving), so the
 * fluid the pump is working on decides as well.
 *
 * <p>Separate from {@link FluidPumpBlockEntityMixin}: {@code ImmersiveEngineeringMixinPlugin} checks this one on its
 * own, so if Immersive Engineering changes this part of the pump only the cobblestone handling switches itself off.
 */
@Mixin(targets = "blusunrize.immersiveengineering.common.blocks.metal.FluidPumpBlockEntity", remap = false)
public abstract class FluidPumpCobbleMixin {

    @Shadow(remap = false)
    @Final
    private List<BlockPos> closedList;

    // The fluid the pump's area search is working on; null until a search has found one
    @Shadow(remap = false)
    private Fluid searchFluid;

    // Position of the block about to be taken, set only when its fluid is finite. Not saved, one per pump
    @Unique
    private BlockPos fluidcompat$finiteDrainTarget;

    // Second of the two Utils.drainFluidBlock calls in tickServer: the first only simulates, this one really takes the block
    @Inject(
            method = "tickServer()V",
            at = @At(value = "INVOKE",
                    target = "Lblusunrize/immersiveengineering/common/util/Utils;drainFluidBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/neoforged/neoforge/fluids/FluidStack;",
                    ordinal = 1,
                    remap = false),
            remap = false)
    private void fluidcompat$rememberFiniteDrain(CallbackInfo callback) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        this.fluidcompat$finiteDrainTarget = FinitePumpRules.finiteDrainTarget(level, this.closedList);
    }

    // The single setBlockAndUpdate call in tickServer is the cobblestone placement
    @Redirect(
            method = "tickServer()V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    remap = false),
            remap = false)
    private boolean fluidcompat$skipCobbleForFiniteFluid(Level level, BlockPos pos, BlockState state) {
        BlockPos finiteTarget = this.fluidcompat$finiteDrainTarget;
        this.fluidcompat$finiteDrainTarget = null;
        if (FinitePumpRules.shouldSkipCobble(finiteTarget, pos, this.searchFluid)) {
            return false;
        }
        return level.setBlockAndUpdate(pos, state);
    }
}
