package me.thanhtnt2.fluidcompat.compat.enderio.mixin.impl;

import me.thanhtnt2.fluidcompat.compat.CompatMixinState;
import me.thanhtnt2.fluidcompat.compat.enderio.EnderIoThinLayer;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets the Ender IO Drain take thin Flowing Fluids layers, see {@link EnderIoThinLayer}.
 *
 * <p>The five injectors work as ONE change: if only some of them applied, a layer would be accepted as a source and
 * then removed or paid the Drain's own way, which creates fluid. So they keep Mixin's default {@code require = 1}
 * (the plugin has already checked every target in the bytecode), and the whole mixin is all or nothing. A failed
 * injector is a loud log line and a skipped mixin, never a silent half hook.
 *
 * <p>Mojang names at runtime on NeoForge 1.21.1, so nothing here needs remapping.
 */
@Pseudo
@Mixin(targets = "com.enderio.enderio.content.machines.drain.DrainBlockEntity", remap = false)
public class DrainBlockEntityMixin {

    private static final String HOOK_ID = "enderio.drain";

    /** Millibuckets the world paid for the layer taken in the current step; -1 when the step has nothing to correct. */
    @Unique
    private int fluidcompat$paidMillibuckets = -1;

    @Redirect(
            method = {"isActive", "drainFluids"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;isSource()Z", remap = false),
            remap = false)
    private boolean fluidcompat$isSourceForDrain(FluidState state) {
        return EnderIoThinLayer.isSourceForDrain(state);
    }

    @Redirect(
            method = {"isActive", "drainFluids"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getType()Lnet/minecraft/world/level/material/Fluid;", remap = false),
            remap = false)
    private Fluid fluidcompat$fluidForDrain(FluidState state) {
        return EnderIoThinLayer.fluidForDrain(state);
    }

    /**
     * The Drain's "replace this block with air". A source block gets exactly that, as before. A thin layer is taken
     * through the finite fluid service instead (air would also be the wrong block for a layer that is not a source).
     */
    @Redirect(
            method = "drainFluids",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", remap = false),
            remap = false)
    private boolean fluidcompat$removeCell(Level level, BlockPos pos, BlockState replacement, int flags) {
        FluidState before = level.getFluidState(pos);
        if (before.isSource() || before.isEmpty() || EnderIoThinLayer.fluidForDrain(before) == before.getType()) {
            this.fluidcompat$paidMillibuckets = -1;
            return level.setBlock(pos, replacement, flags);
        }
        if (CompatMixinState.markHookReached(HOOK_ID)) {
            FluidCompatLog.info("[enderio] Drain took a thin layer for the first time at " + pos.toShortString());
        }
        this.fluidcompat$paidMillibuckets = EnderIoThinLayer.takeLayer(level, pos, fluidcompat$machineId());
        return true;
    }

    /** The one fill that really moves fluid into the tank: for a thin layer it carries exactly what the world paid. */
    @ModifyArg(
            method = "drainFluids",
            at = @At(value = "INVOKE", target = "Lcom/enderio/enderio/foundation/io/fluid/TankAccess;fill(Lcom/enderio/enderio/foundation/attachment/FluidTankUser;Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I", ordinal = 1, remap = false),
            index = 1,
            remap = false)
    private FluidStack fluidcompat$fillWhatWasPaid(FluidStack bucket) {
        int paid = this.fluidcompat$paidMillibuckets;
        this.fluidcompat$paidMillibuckets = -1;
        if (paid < 0) {
            return bucket;
        }
        return paid == 0 ? FluidStack.EMPTY : bucket.copyWithAmount(Math.min(paid, bucket.getAmount()));
    }

    @Unique
    private ResourceLocation fluidcompat$machineId() {
        BlockEntity self = (BlockEntity) (Object) this;
        ResourceLocation key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(self.getType());
        return key != null ? key : ResourceLocation.fromNamespaceAndPath("enderio", "drain");
    }
}
