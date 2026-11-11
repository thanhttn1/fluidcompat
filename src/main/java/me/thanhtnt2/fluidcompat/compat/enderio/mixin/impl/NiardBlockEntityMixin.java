package me.thanhtnt2.fluidcompat.compat.enderio.mixin.impl;

import me.thanhtnt2.fluidcompat.compat.CompatMixinState;
import me.thanhtnt2.fluidcompat.compat.enderio.EnderIoNiardRefund;
import me.thanhtnt2.fluidcompat.compat.enderio.ThinLayerRule;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes the Ender IO Niard pay only for the fluid the world really gains when it fills over a thin layer of its own
 * fluid, see {@link EnderIoNiardRefund}. Three injectors that work as one change (the fluid being placed, the moment
 * before the cell is replaced, the bucket that is then taken from the tank), so they keep Mixin's default
 * {@code require = 1} and the mixin is all or nothing. Worst case of a half hook: no refund, never extra fluid.
 */
@Pseudo
@Mixin(targets = "com.enderio.enderio.content.machines.niard.NiardBlockEntity", remap = false)
public class NiardBlockEntityMixin {

    private static final String HOOK_ID = "enderio.niard";

    @Unique
    private Fluid fluidcompat$placedFluid;

    @Unique
    private int fluidcompat$heldLevels;

    /** Notes which fluid the Niard is about to place; the fluid itself is passed on untouched. */
    @Redirect(
            method = "placeFluid",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/Fluid;defaultFluidState()Lnet/minecraft/world/level/material/FluidState;", remap = false),
            remap = false)
    private FluidState fluidcompat$notePlacedFluid(Fluid fluid) {
        this.fluidcompat$placedFluid = fluid;
        return fluid.defaultFluidState();
    }

    /** Right before the cell is replaced: how much of the new bucket was already in the world there? */
    @Redirect(
            method = "placeFluid",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", remap = false),
            remap = false)
    private boolean fluidcompat$replaceCell(Level level, BlockPos pos, BlockState placed, int flags) {
        this.fluidcompat$heldLevels = EnderIoNiardRefund.levelsHeldBeforeReplacing(level, pos, this.fluidcompat$placedFluid);
        return level.setBlock(pos, placed, flags);
    }

    /** The bucket taken from the tank after a placement: whole minus what the layer already held. */
    @ModifyArg(
            method = "consumeResources",
            at = @At(value = "INVOKE", target = "Lcom/enderio/enderio/foundation/io/fluid/TankAccess;drain(Lcom/enderio/enderio/foundation/attachment/FluidTankUser;ILnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/neoforged/neoforge/fluids/FluidStack;", remap = false),
            index = 1,
            remap = false)
    private int fluidcompat$chargeOnlyTheGain(int bucket) {
        int held = this.fluidcompat$heldLevels;
        this.fluidcompat$heldLevels = 0;
        this.fluidcompat$placedFluid = null;
        if (held <= 0) {
            return bucket;
        }
        if (CompatMixinState.markHookReached(HOOK_ID)) {
            FluidCompatLog.info("[enderio] Niard charged only the gain over a thin layer for the first time");
        }
        return Math.min(bucket, ThinLayerRule.niardChargeMillibuckets(held));
    }
}
