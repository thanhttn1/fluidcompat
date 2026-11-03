package me.thanhtnt2.fluidcompat.compat.flowingfluids.patches;

import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.Fluid;

/**
 * The only entry points the patch mixins call. Everything that can fail is caught here, so a problem in a patch hands
 * control back to the original Flowing Fluids code instead of crashing a fluid tick.
 */
public final class PatchHooks {

    private PatchHooks() {
    }

    /**
     * @return true when the listeners were notified and the original method must be skipped; false when something
     *         went wrong and the original method should run as if there were no patch
     */
    public static boolean notifyFlowListeners(
            LevelAccessor level, BlockPos fluidPos, Direction direction, Fluid fluid, boolean downAlso) {
        try {
            FlowListenerDispatch.notifyListeners(level, fluidPos, direction, fluid, downAlso);
            return true;
        } catch (RuntimeException | LinkageError error) {
            // LinkageError covers a Flowing Fluids interface that changed shape
            FluidCompatLog.errorThrottled("ff-patch-flow-listeners", "Flowing Fluids flow listener patch failed; the original code runs instead", error);
            return false;
        }
    }

    /** @return true when the request must be refused because it asks to remove a negative amount */
    public static boolean isNegativeRemoval(BlockPos pos, int maxAmount) {
        if (maxAmount >= 0) {
            return false;
        }
        // The stack trace is the point: it shows which mod made the bad call
        FluidCompatLog.errorThrottled("ff-patch-negative-removal",
                "A mod asked Flowing Fluids to remove " + maxAmount + " level(s) of fluid at " + (pos == null ? "unknown position" : pos.toShortString())
                        + " (a negative amount would create fluid). The request was refused.",
                new IllegalArgumentException("negative maxAmount " + maxAmount));
        return true;
    }
}
