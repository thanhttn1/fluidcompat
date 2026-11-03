package me.thanhtnt2.fluidcompat.compat.immersiveengineering;

import java.util.List;

import me.thanhtnt2.fluidcompat.api.FiniteFluidService;
import me.thanhtnt2.fluidcompat.compat.common.CompatGate;
import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import me.thanhtnt2.fluidcompat.api.FluidIntakeRequest;
import me.thanhtnt2.fluidcompat.api.FluidIntakeResult;
import me.thanhtnt2.fluidcompat.config.FluidCompatModulesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Lets the Immersive Engineering Fluid Pump empty a pond completely.
 *
 * <p>The pump only ever sees source blocks. With Flowing Fluids a draining pond ends up as thin layers of 1 to 7
 * levels, which are not source blocks and are even a different {@code Fluid} object ("flowing water" instead of
 * "water"), so the pump stopped with a film of water left. Two things change for finite fluids:
 * <ul>
 *   <li>the pump's area search treats a thin layer as the same fluid as a source block ({@link #normalizedType}),</li>
 *   <li>taking such a cell removes exactly the levels it holds and hands over exactly that many millibuckets
 *       ({@link #tryDrain}), paid by the world through {@code FiniteFluidService}, so no water is made or lost.</li>
 * </ul>
 * Whole source blocks stay entirely Immersive Engineering's own business.
 */
public final class ThinFluidDrain {

    private static final ResourceLocation MACHINE_ID =
            ResourceLocation.fromNamespaceAndPath("immersiveengineering", "fluid_pump");

    private ThinFluidDrain() {
    }

    /**
     * The fluid the pump should compare against: the source variant for a thin layer of a finite fluid, otherwise the
     * cell's own type, which is exactly what the pump asked for before.
     */
    public static Fluid normalizedType(FluidState fluidState) {
        Fluid type = fluidState.getType();
        if (fluidState.isEmpty() || fluidState.isSource() || PumpHookHealth.isTripped()
                || !(type instanceof FlowingFluid flowing)) {
            return type;
        }
        try {
            if (FluidCompatApi.isReady()
                    && CompatGate.masterEnabled()
                    && FluidCompatModulesConfig.IE_PUMP_DRAIN_THIN.get()
                    && FluidCompatApi.service().isFinite(type)) {
                return flowing.getSource();
            }
        } catch (RuntimeException | LinkageError error) {
            PumpHookHealth.recordFailure(error);
        }
        return type;
    }

    /**
     * Answers the pump's {@code Utils.drainFluidBlock} for a thin layer of a finite fluid.
     *
     * @return the fluid taken, or null when this is not a case for Fluid Compat and Immersive Engineering should
     *         answer as usual
     */
    public static FluidStack tryDrain(Level level, BlockPos pos, IFluidHandler.FluidAction action) {
        if (PumpHookHealth.isTripped() || !(level instanceof ServerLevel serverLevel) || !FluidCompatApi.isReady()) {
            return null;
        }
        try {
            return drainThinLayer(serverLevel, pos, action);
        } catch (RuntimeException | LinkageError error) {
            PumpHookHealth.recordFailure(error);
            return null;
        }
    }

    private static FluidStack drainThinLayer(ServerLevel level, BlockPos pos, IFluidHandler.FluidAction action) {
        if (!CompatGate.masterEnabled() || !FluidCompatModulesConfig.IE_PUMP_DRAIN_THIN.get()) {
            return null;
        }
        FluidState fluidState = level.getFluidState(pos);
        // Plain fluid blocks only: a waterlogged block keeps its own rules, and a source block is Immersive Engineering's
        if (fluidState.isEmpty() || fluidState.isSource() || !(level.getBlockState(pos).getBlock() instanceof LiquidBlock)) {
            return null;
        }
        FiniteFluidService service = FluidCompatApi.service();
        if (!(fluidState.getType() instanceof FlowingFluid flowing) || !service.isFinite(flowing)) {
            return null;
        }
        // The tank holds the source variant ("water"), never "flowing water", so it stacks with what is already in it
        Fluid tankFluid = flowing.getSource();
        int levels = fluidState.getAmount();
        if (action.simulate()) {
            return new FluidStack(tankFluid, levels * FluidCompatApi.MILLIBUCKETS_PER_LEVEL);
        }
        return takeLevels(level, pos, tankFluid, levels, service);
    }

    private static FluidStack takeLevels(ServerLevel level, BlockPos pos, Fluid tankFluid, int levels, FiniteFluidService service) {
        // Flowing Fluids works with the fluid as a whole, so the source variant is what every bridge hands it
        FluidIntakeResult result = service.requestIntake(
                new FluidIntakeRequest(level, pos, MACHINE_ID, tankFluid, levels, List.of(pos.immutable())));
        if (!result.managed()) {
            return null;
        }
        // The pump gets what the world really gave up, even if that is less than the cell held a moment ago
        int drainedMillibuckets = result.drainedLevels() * FluidCompatApi.MILLIBUCKETS_PER_LEVEL;
        return drainedMillibuckets > 0 ? new FluidStack(tankFluid, drainedMillibuckets) : FluidStack.EMPTY;
    }
}
