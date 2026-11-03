package me.thanhtnt2.fluidcompat.compat.immersiveengineering;

import java.util.List;

import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import me.thanhtnt2.fluidcompat.compat.common.CompatGate;
import me.thanhtnt2.fluidcompat.config.FluidCompatModulesConfig;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * The decisions behind the Immersive Engineering Fluid Pump hooks.
 *
 * <p><b>Endless source.</b> The pump asks {@code FluidState.canConvertToSource} in two places. When the answer is
 * yes, it fills its tank for free from any source block that has two source neighbours (vanilla's "infinite water"
 * rule) and its area search skips that fluid completely. Flowing Fluids does not change that answer, so with the
 * gamerule on the pump would make water out of nothing. Answering "no" for finite fluids sends the pump down the
 * path it already uses for lava or oil: it takes real source blocks out of the world, 1000 mB per block, which keeps
 * the water count.
 *
 * <p><b>Cobblestone.</b> On that path the pump replaces each emptied block with cobblestone. For a finite fluid
 * that would leave stone where the pond should shrink, so the replacement is skipped and Flowing Fluids refills
 * the gap.
 *
 */
public final class FinitePumpRules {

    private FinitePumpRules() {
    }

    /**
     * @return what the pump should believe: true only if the fluid really may be an endless source
     */
    public static boolean mayConvertToSource(FluidState fluidState, Level level, BlockPos pos) {
        boolean originalAnswer = fluidState.canConvertToSource(level, pos);
        if (!originalAnswer || PumpHookHealth.isTripped()) {
            return originalAnswer; // Already finite (lava, oil) or switched off: nothing to change
        }
        try {
            return !(CompatGate.masterEnabled() && FluidCompatModulesConfig.IE_PUMP_FINITE.get() && isFinite(fluidState));
        } catch (RuntimeException | LinkageError error) {
            PumpHookHealth.recordFailure(error);
            return originalAnswer;
        }
    }

    /**
     * Called just before the pump takes the block at the end of its work list. Returns that position when its fluid
     * is finite and the cobblestone replacement should be skipped, otherwise null.
     */
    public static BlockPos finiteDrainTarget(Level level, List<BlockPos> workList) {
        if (PumpHookHealth.isTripped() || level == null || workList.isEmpty()) {
            return null;
        }
        try {
            if (!CompatGate.masterEnabled() || !FluidCompatModulesConfig.IE_PUMP_NO_COBBLE.get()) {
                return null;
            }
            // The pump works from the end of its list, see FluidPumpBlockEntity.tickServer
            BlockPos target = workList.get(workList.size() - 1);
            return isFinite(level.getFluidState(target)) ? target.immutable() : null;
        } catch (RuntimeException | LinkageError error) {
            PumpHookHealth.recordFailure(error);
            return null;
        }
    }

    /**
     * Decides whether the cobblestone the pump is about to place must be skipped.
     *
     * <p>Two cases, either is enough. The block it sits on held a finite fluid a moment ago ({@code finiteDrainTarget}).
     * Or the pump is working on a finite fluid at all ({@code pumpedFluid}, the fluid its area search found): the
     * second case matters because the pump's work list is made some time before it is used, and with Flowing Fluids a
     * thin layer can have flowed away in between. The pump still "takes" such an empty block and puts cobblestone
     * there, so looking at the block alone is not enough.
     */
    public static boolean shouldSkipCobble(BlockPos finiteDrainTarget, BlockPos cobblePos, Fluid pumpedFluid) {
        if (PumpHookHealth.isTripped()) {
            return false;
        }
        try {
            boolean tookFiniteFluidHere = finiteDrainTarget != null && finiteDrainTarget.equals(cobblePos);
            boolean pumpingFiniteFluid = !tookFiniteFluidHere && pumpsFiniteFluid(pumpedFluid);
            if ((tookFiniteFluidHere || pumpingFiniteFluid) && FluidCompatLog.isDetailLoggingActive()) {
                FluidCompatLog.debugThrottled("immersiveengineering-no-cobble",
                        "[immersiveengineering] Fluid Pump: no cobblestone placed at {} ({})", cobblePos.toShortString(),
                        tookFiniteFluidHere ? "finite fluid taken" : "block already empty, pump works on a finite fluid");
            }
            return tookFiniteFluidHere || pumpingFiniteFluid;
        } catch (RuntimeException | LinkageError error) {
            PumpHookHealth.recordFailure(error);
            return false;
        }
    }

    private static boolean pumpsFiniteFluid(Fluid pumpedFluid) {
        return pumpedFluid != null
                && pumpedFluid != Fluids.EMPTY
                && CompatGate.masterEnabled()
                && FluidCompatModulesConfig.IE_PUMP_NO_COBBLE.get()
                && FluidCompatApi.isReady()
                && FluidCompatApi.service().isFinite(pumpedFluid);
    }

    private static boolean isFinite(FluidState fluidState) {
        return !fluidState.isEmpty()
                && FluidCompatApi.isReady()
                && FluidCompatApi.service().isFinite(fluidState.getType());
    }
}
