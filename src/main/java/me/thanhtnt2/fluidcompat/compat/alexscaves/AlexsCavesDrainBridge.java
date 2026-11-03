package me.thanhtnt2.fluidcompat.compat.alexscaves;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import me.thanhtnt2.fluidcompat.FluidCompat;
import me.thanhtnt2.fluidcompat.api.FiniteFluidService;
import me.thanhtnt2.fluidcompat.compat.common.CompatGate;
import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import me.thanhtnt2.fluidcompat.api.FluidIntakeRequest;
import me.thanhtnt2.fluidcompat.api.FluidIntakeResult;
import me.thanhtnt2.fluidcompat.config.FluidCompatModulesConfig;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

/**
 * Makes the Alex's Caves Drain work with finite fluids.
 *
 * <p>The original Drain removes fluid blocks one by one and puts the same number of full blocks below. With
 * Flowing Fluids a cell holds 1 to 8 levels, so that "one block" arithmetic neither matches what was removed
 * nor what Flowing Fluids allows. Here the Drain instead asks {@link FiniteFluidService} to take levels from
 * the fluid it can reach and places exactly that many levels below.
 *
 * <p>Where the Drain finds its fluid: directly above it; or, because a Copper Valve is waterlogged in the original
 * and Flowing Fluids ignores waterlogging, around a waterloggable block above it; or around a Copper Valve that
 * sits on one of its four sides.
 *
 * <p>Called from the mixin on the server thread. Has no reference to Alex's Caves classes.
 */
public final class AlexsCavesDrainBridge {

    private static final ResourceLocation FALLBACK_MACHINE_ID =
            ResourceLocation.fromNamespaceAndPath(FluidCompat.MODID, "unknown_drain");

    private static final String ALEXS_CAVES_NAMESPACE = "alexscaves";
    private static final String COPPER_VALVE_PATH = "copper_valve";

    // Cells around a valve that may hold the fluid it is "looking at"; never downwards, that is the drain's own side
    private static final Direction[] VALVE_PROBE_DIRECTIONS =
            {Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final Direction[] SIDE_DIRECTIONS =
            {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    /** Where the fluid is taken from, and what kind of fluid it is. */
    private record FluidSource(BlockPos origin, FlowingFluid fluid) {
    }

    private AlexsCavesDrainBridge() {
    }

    /**
     * @return true when Fluid Compat did the work and the original Drain logic must be skipped;
     *         false when the original logic should run unchanged
     */
    public static boolean tryHandle(BlockState drainState, Level level, BlockPos drainPos) {
        try {
            return handle(drainState, level, drainPos);
        } catch (RuntimeException | LinkageError error) {
            // Fail open. handle() only lets an error reach this point before it has changed the world
            // (LinkageError covers a Flowing Fluids API that changed shape), so the original logic may run
            FluidCompatLog.errorThrottled("alexscaves-drain",
                    "Alex's Caves Drain hook failed at " + drainPos.toShortString(), error);
            return false;
        }
    }

    private static boolean handle(BlockState drainState, Level level, BlockPos drainPos) {
        if (!(level instanceof ServerLevel serverLevel) || !FluidCompatApi.isReady()) {
            return false;
        }
        if (!CompatGate.masterEnabled() || !FluidCompatModulesConfig.ALEXS_DRAIN_ENABLED.get()) {
            return false;
        }
        Optional<FluidSource> located = locateFluidSource(serverLevel, drainPos);
        if (located.isEmpty()) {
            logSkip(drainPos, () -> "no fluid above the Drain or beside a Copper Valve next to it");
            return false;
        }
        FluidSource source = located.get();
        FiniteFluidService service = FluidCompatApi.service();
        if (!service.isFinite(source.fluid())) {
            return false;
        }

        DrainFillPlan plan = DrainFillPlan.create(serverLevel, drainPos, source.fluid(),
                FluidCompatModulesConfig.ALEXS_DRAIN_MAX_LEVELS_PER_CYCLE.get(),
                FluidCompatModulesConfig.ALEXS_DRAIN_FILL_SPREAD.get());
        if (plan.isEmpty()) {
            // Nowhere to put the fluid: taking it anyway would destroy it, so wait and look again later
            logSkip(drainPos, () -> "no room below the Drain (block below: " + describeBlockBelow(serverLevel, drainPos) + ")");
            scheduleNextCycle(serverLevel, drainPos, drainState);
            return true;
        }

        FluidIntakeResult result = service.requestIntake(buildRequest(serverLevel, drainState, drainPos, source, plan));
        if (!result.managed()) {
            return false;
        }
        // From here the world has been charged: an error must not hand the Drain back to the original logic,
        // which would move the same fluid a second time
        try {
            moveFluid(serverLevel, drainPos, source.fluid(), plan, result);
        } catch (RuntimeException | LinkageError error) {
            FluidCompatLog.errorThrottled("alexscaves-drain-move",
                    "Alex's Caves Drain could not place the fluid it took at " + drainPos.toShortString(), error);
        }
        scheduleNextCycle(serverLevel, drainPos, drainState);
        return true;
    }

    private static Optional<FluidSource> locateFluidSource(ServerLevel level, BlockPos drainPos) {
        BlockPos above = drainPos.above();
        Optional<FluidSource> direct = readFluidSource(level, above);
        if (direct.isPresent()) {
            return direct;
        }
        // A waterloggable block above (the Copper Valve) does not hold water itself while Flowing Fluids ignores
        // waterlogging, so look at the cells around it instead
        if (level.getBlockState(above).getBlock() instanceof SimpleWaterloggedBlock) {
            Optional<FluidSource> besideAbove = probeAround(level, above, drainPos);
            if (besideAbove.isPresent()) {
                return besideAbove;
            }
        }
        return locateBesideSideValves(level, drainPos);
    }

    // The valve works as a redstone switch from any side of the Drain, so it must be allowed to hand out fluid from there too
    private static Optional<FluidSource> locateBesideSideValves(ServerLevel level, BlockPos drainPos) {
        for (Direction side : SIDE_DIRECTIONS) {
            BlockPos valvePos = drainPos.relative(side);
            if (!isCopperValve(level.getBlockState(valvePos))) {
                continue;
            }
            Optional<FluidSource> inValve = readFluidSource(level, valvePos);
            if (inValve.isPresent()) {
                return inValve;
            }
            Optional<FluidSource> aroundValve = probeAround(level, valvePos, drainPos);
            if (aroundValve.isPresent()) {
                return aroundValve;
            }
        }
        return Optional.empty();
    }

    private static Optional<FluidSource> probeAround(ServerLevel level, BlockPos center, BlockPos excluded) {
        for (Direction direction : VALVE_PROBE_DIRECTIONS) {
            BlockPos probed = center.relative(direction);
            if (probed.equals(excluded)) {
                continue;
            }
            Optional<FluidSource> found = readFluidSource(level, probed);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private static boolean isCopperValve(BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return ALEXS_CAVES_NAMESPACE.equals(key.getNamespace()) && COPPER_VALVE_PATH.equals(key.getPath());
    }

    private static Optional<FluidSource> readFluidSource(ServerLevel level, BlockPos pos) {
        FluidState fluidState = level.getFluidState(pos);
        if (fluidState.isEmpty() || !(fluidState.getType() instanceof FlowingFluid flowing)) {
            return Optional.empty();
        }
        return Optional.of(new FluidSource(pos.immutable(), flowing));
    }

    private static FluidIntakeRequest buildRequest(
            ServerLevel level, BlockState drainState, BlockPos drainPos, FluidSource source, DrainFillPlan plan) {
        return new FluidIntakeRequest(level, drainPos, machineIdOf(drainState), source.fluid(),
                plan.totalLevels(), collectSourcePositions(level, source));
    }

    // Highest cell of the fluid column first, so the surface is lowered before the bottom is touched
    private static List<BlockPos> collectSourcePositions(ServerLevel level, FluidSource source) {
        BlockPos.MutableBlockPos cursor = source.origin().mutable();
        while (cursor.getY() < level.getMaxBuildHeight() - 1 && !level.getFluidState(cursor.above()).isEmpty()) {
            cursor.move(0, 1, 0);
        }
        List<BlockPos> sources = new ArrayList<>(2);
        sources.add(cursor.immutable());
        // The origin is a second candidate only when it is a real fluid block; a waterlogged valve is not one
        boolean originIsFluidBlock = level.getBlockState(source.origin()).getBlock() instanceof LiquidBlock;
        if (originIsFluidBlock && !source.origin().equals(cursor)) {
            sources.add(source.origin());
        }
        return sources;
    }

    private static void moveFluid(
            ServerLevel level, BlockPos drainPos, FlowingFluid fluid, DrainFillPlan plan, FluidIntakeResult result) {
        int drainedLevels = result.drainedLevels();
        if (drainedLevels <= 0) {
            logSkip(drainPos, () -> "the finite-fluid backend removed 0 level(s); nothing was moved");
            return;
        }
        boolean playEffects = FluidCompatModulesConfig.ALEXS_DRAIN_WORLD_EFFECTS.get();
        if (playEffects) {
            DrainWorldEffects.play(DrainWorldEffects.FLUID_TAKEN, drainPos);
        }
        int placedLevels = plan.apply(level, fluid, drainedLevels);
        if (playEffects && placedLevels > 0) {
            DrainWorldEffects.play(DrainWorldEffects.FLUID_RELEASED, drainPos);
        }
        if (FluidCompatLog.isDetailLoggingActive()) {
            FluidCompatLog.debugThrottled("alexscaves-drain-" + drainPos.asLong(),
                    "[alexscaves] Drain at {} moved {} level(s): drained={} placed={}",
                    drainPos.toShortString(), result.requestedLevels(), drainedLevels, placedLevels);
        }
    }

    private static void scheduleNextCycle(ServerLevel level, BlockPos drainPos, BlockState drainState) {
        level.scheduleTick(drainPos, drainState.getBlock(), FluidCompatModulesConfig.ALEXS_DRAIN_INTERVAL_TICKS.get());
    }

    // Says why a Drain did nothing; the text is only built while verbose/debug logging is on
    private static void logSkip(BlockPos drainPos, Supplier<String> reason) {
        if (!FluidCompatLog.isDetailLoggingActive()) {
            return;
        }
        FluidCompatLog.debugThrottled("alexscaves-drain-skip-" + drainPos.asLong(),
                "[alexscaves] Drain at {}: {}", drainPos.toShortString(), reason.get());
    }

    private static String describeBlockBelow(ServerLevel level, BlockPos drainPos) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(level.getBlockState(drainPos.below()).getBlock());
        return key != null ? key.toString() : "unknown";
    }

    private static ResourceLocation machineIdOf(BlockState drainState) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(drainState.getBlock());
        return key != null ? key : FALLBACK_MACHINE_ID;
    }
}
