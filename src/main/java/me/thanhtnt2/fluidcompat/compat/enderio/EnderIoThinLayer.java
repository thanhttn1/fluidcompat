package me.thanhtnt2.fluidcompat.compat.enderio;

import java.util.List;

import me.thanhtnt2.fluidcompat.api.FiniteFluidService;
import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import me.thanhtnt2.fluidcompat.api.FluidIntakeRequest;
import me.thanhtnt2.fluidcompat.api.FluidIntakeResult;
import me.thanhtnt2.fluidcompat.compat.common.CompatGate;
import me.thanhtnt2.fluidcompat.config.FluidCompatModulesConfig;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/**
 * The Ender IO Drain only ever sees source blocks. With Flowing Fluids a draining pond ends up as thin layers of 1 to 7
 * levels, which are not source blocks and are even a different {@code Fluid} object ("flowing water" instead of
 * "water"), so the Drain stopped with a film of water left, and stopped completely when the block right under it
 * became such a layer. For finite fluids the Drain's code is steered in three places (see the mixin):
 * <ul>
 *   <li>a thin layer counts as a source ({@link #isSourceForDrain}),</li>
 *   <li>it is reported as the source variant, so the tank stacks it with what it already holds ({@link #fluidForDrain}),</li>
 *   <li>taking it removes exactly the levels it holds through {@code FiniteFluidService} and the Drain receives exactly
 *       that many millibuckets ({@link #takeLayer}), so no fluid is made or lost.</li>
 * </ul>
 * Source blocks stay entirely Ender IO's own business. Every answer fails open: a problem gives the answer the Drain
 * would have got without Fluid Compat.
 */
public final class EnderIoThinLayer {

    private EnderIoThinLayer() {
    }

    /** What the Drain's "is it a source?" question gets. */
    public static boolean isSourceForDrain(FluidState state) {
        return state.isSource() || isThinLayer(state);
    }

    /** What the Drain's "which fluid is it?" question gets: the source variant for a thin layer, else the cell's own type. */
    public static Fluid fluidForDrain(FluidState state) {
        Fluid type = state.getType();
        if (isThinLayer(state) && type instanceof FlowingFluid flowing) {
            return flowing.getSource();
        }
        return type;
    }

    /**
     * Whether this cell is a thin layer of a finite fluid that the Drain should take. Decided from the fluid state
     * alone, so the three hooks of one drain step always agree.
     */
    static boolean isThinLayer(FluidState state) {
        if (state.isEmpty() || state.isSource() || !(state.getType() instanceof FlowingFluid flowing)) {
            return false;
        }
        try {
            return CompatGate.masterEnabled()
                    && FluidCompatModulesConfig.ENDERIO_ENABLED.get()
                    && FluidCompatModulesConfig.ENDERIO_DRAIN_THIN_WATER.get()
                    && FluidCompatApi.isReady()
                    && FluidCompatApi.service().isFinite(flowing.getSource())
                    && ThinLayerRule.isUsableLayer(state.getAmount());
        } catch (RuntimeException | LinkageError error) {
            FluidCompatLog.errorThrottled("enderio-thin-layer", "Ender IO thin layer check failed", error);
            return false;
        }
    }

    /**
     * Takes a thin layer out of the world in place of the Drain's own "replace the block with air".
     *
     * @return millibuckets the world really gave up (possibly 0), which is all the Drain may receive
     */
    public static int takeLayer(Level level, BlockPos pos, ResourceLocation machineId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0;
        }
        try {
            FluidState state = level.getFluidState(pos);
            if (!isThinLayer(state) || !(state.getType() instanceof FlowingFluid flowing)) {
                return 0;
            }
            // Flowing Fluids works with the fluid as a whole, so the source variant is what every bridge hands it
            FiniteFluidService service = FluidCompatApi.service();
            FluidIntakeResult result = service.requestIntake(new FluidIntakeRequest(
                    serverLevel, pos, machineId, flowing.getSource(), state.getAmount(), List.of(pos.immutable())));
            if (!result.managed()) {
                return 0;
            }
            return Math.max(0, result.drainedLevels()) * FluidCompatApi.MILLIBUCKETS_PER_LEVEL;
        } catch (RuntimeException | LinkageError error) {
            FluidCompatLog.errorThrottled("enderio-thin-layer", "Ender IO thin layer intake failed", error);
            return 0;
        }
    }

    /** True when the block at this position is a plain fluid block, the only thing a thin layer can be. */
    static boolean isPlainFluidBlock(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof LiquidBlock;
    }
}
