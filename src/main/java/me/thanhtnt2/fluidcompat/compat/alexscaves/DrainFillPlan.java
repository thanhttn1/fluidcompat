package me.thanhtnt2.fluidcompat.compat.alexscaves;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

/**
 * Decides where the fluid taken from above a Drain is put back below it, measured in fluid levels
 * (1/8 bucket). Planning never changes the world, so the Drain can first ask the finite-fluid service
 * whether it may take the fluid and only then place it: fluid is moved, never created or lost.
 *
 * <p>Mirrors the original Drain, which refills the cavity below from its floor upwards and spreads at most
 * a few blocks sideways; the difference is that partly filled cells are topped up instead of overwritten.
 */
final class DrainFillPlan {

    private static final int MAX_DROP_SCAN = 256; // Safety cap for very deep shafts below a Drain
    private static final int MAX_CELLS_PER_LAYER = 256;
    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    /** One cell that receives fluid: it currently holds {@code currentLevels} and gets {@code addedLevels} more. */
    record Step(BlockPos pos, int currentLevels, int addedLevels) {
    }

    private final List<Step> steps;
    private final int totalLevels;

    private DrainFillPlan(List<Step> steps, int totalLevels) {
        this.steps = steps;
        this.totalLevels = totalLevels;
    }

    int totalLevels() {
        return totalLevels;
    }

    boolean isEmpty() {
        return steps.isEmpty();
    }

    /** Fluid levels in the cell if it can take {@code fluid}, or -1 if it is blocked or holds something else. */
    private static int levelsAt(ServerLevel level, BlockPos pos, FlowingFluid fluid) {
        if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos)) {
            return -1;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return 0;
        }
        FluidState fluidState = state.getFluidState();
        // Only plain fluid blocks count; waterlogged blocks are left alone so no block state is ever overwritten
        boolean plainSameFluid = state.getBlock() instanceof LiquidBlock && fluid.isSame(fluidState.getType());
        return plainSameFluid ? fluidState.getAmount() : -1;
    }

    static DrainFillPlan create(ServerLevel level, BlockPos drainPos, FlowingFluid fluid, int maxLevels, int maxSpread) {
        BlockPos firstBelow = drainPos.below();
        if (levelsAt(level, firstBelow, fluid) < 0) {
            return new DrainFillPlan(List.of(), 0);
        }
        BlockPos floor = findFloor(level, firstBelow, fluid);

        List<Step> steps = new ArrayList<>();
        int budget = maxLevels;
        for (int y = findFirstLayerToFill(level, firstBelow, floor, fluid); y <= firstBelow.getY() && budget > 0; y++) {
            BlockPos column = new BlockPos(drainPos.getX(), y, drainPos.getZ());
            budget -= collectLayer(level, column, fluid, maxSpread, budget, steps);
        }
        return new DrainFillPlan(List.copyOf(steps), maxLevels - budget);
    }

    /**
     * Layers under the highest completely full cell of the column are taken as full and skipped. Without this,
     * a Drain above an already full tank would re-examine every layer of it each cycle for nothing.
     */
    private static int findFirstLayerToFill(ServerLevel level, BlockPos firstBelow, BlockPos floor, FlowingFluid fluid) {
        BlockPos.MutableBlockPos cursor = firstBelow.mutable();
        while (cursor.getY() > floor.getY()) {
            if (levelsAt(level, cursor, fluid) == FluidCompatApi.LEVELS_PER_BUCKET) {
                return cursor.getY();
            }
            cursor.move(Direction.DOWN);
        }
        return floor.getY();
    }

    private static BlockPos findFloor(ServerLevel level, BlockPos start, FlowingFluid fluid) {
        BlockPos.MutableBlockPos cursor = start.mutable();
        for (int scanned = 0; scanned < MAX_DROP_SCAN; scanned++) {
            if (levelsAt(level, cursor.below(), fluid) < 0) {
                break;
            }
            cursor.move(Direction.DOWN);
        }
        return cursor.immutable();
    }

    /** Breadth-first fill of one horizontal layer, closest cells first. Returns the levels reserved. */
    private static int collectLayer(
            ServerLevel level, BlockPos column, FlowingFluid fluid, int maxSpread, int budget, List<Step> steps) {
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        LongOpenHashSet visited = new LongOpenHashSet();
        queue.add(column);
        visited.add(column.asLong());

        int reserved = 0;
        int visitedCells = 0;
        while (!queue.isEmpty() && visitedCells < MAX_CELLS_PER_LAYER && reserved < budget) {
            BlockPos pos = queue.poll();
            visitedCells++;
            int current = levelsAt(level, pos, fluid);
            if (current < 0) {
                continue;
            }
            int space = FluidCompatApi.LEVELS_PER_BUCKET - current;
            if (space > 0) {
                int added = Math.min(space, budget - reserved);
                steps.add(new Step(pos, current, added));
                reserved += added;
            }
            enqueueNeighbours(pos, column, maxSpread, queue, visited);
        }
        return reserved;
    }

    private static void enqueueNeighbours(
            BlockPos pos, BlockPos column, int maxSpread, java.util.ArrayDeque<BlockPos> queue, LongOpenHashSet visited) {
        for (Direction direction : HORIZONTAL) {
            BlockPos next = pos.relative(direction);
            int distance = Math.abs(next.getX() - column.getX()) + Math.abs(next.getZ() - column.getZ());
            if (distance <= maxSpread && visited.add(next.asLong())) {
                queue.add(next);
            }
        }
    }

    /**
     * Places up to {@code levelsToPlace} levels following the plan.
     *
     * @return the number of levels placed, never more than requested
     */
    int apply(ServerLevel level, FlowingFluid fluid, int levelsToPlace) {
        int remaining = levelsToPlace;
        for (Step step : steps) {
            if (remaining <= 0) {
                break;
            }
            int added = Math.min(step.addedLevels(), remaining);
            level.setBlockAndUpdate(step.pos(), stateWithLevels(fluid, step.currentLevels() + added));
            remaining -= added;
        }
        return levelsToPlace - remaining;
    }

    private static BlockState stateWithLevels(FlowingFluid fluid, int levels) {
        FluidState fluidState = levels >= FluidCompatApi.LEVELS_PER_BUCKET
                ? fluid.getSource(false)
                : fluid.getFlowing(levels, false);
        return fluidState.createLegacyBlock();
    }
}
