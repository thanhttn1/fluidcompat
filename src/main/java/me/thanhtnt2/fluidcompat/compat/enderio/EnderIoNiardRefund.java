package me.thanhtnt2.fluidcompat.compat.enderio;

import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import me.thanhtnt2.fluidcompat.compat.common.CompatGate;
import me.thanhtnt2.fluidcompat.config.FluidCompatModulesConfig;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/**
 * The Niard turns a cell that holds a thin layer of its own fluid into a full source block and pays a whole bucket
 * (1000 mB) for it. The layer already held part of that bucket, so with a finite fluid the world gains less than the
 * Niard paid and the difference vanished. This reports how many levels the cell holds right before it is replaced, so
 * the Niard is charged only for what the world gains.
 *
 * <p>Only the Niard's own fluid counts: a layer of some other fluid that gets replaced is a different matter and is
 * left alone. Fails open: any problem means "no refund", which is the Niard's original behaviour.
 */
public final class EnderIoNiardRefund {

    private EnderIoNiardRefund() {
    }

    /** @return levels held by the cell at {@code pos} if it is a thin layer of {@code placedFluid}, else 0 */
    public static int levelsHeldBeforeReplacing(Level level, BlockPos pos, Fluid placedFluid) {
        try {
            if (placedFluid == null || !CompatGate.masterEnabled()
                    || !FluidCompatModulesConfig.ENDERIO_ENABLED.get()
                    || !FluidCompatModulesConfig.ENDERIO_NIARD_REFUND.get()
                    || !FluidCompatApi.isReady()) {
                return 0;
            }
            FluidState state = level.getFluidState(pos);
            if (state.isEmpty() || state.isSource() || !(state.getType() instanceof FlowingFluid flowing)) {
                return 0;
            }
            if (!flowing.getSource().isSame(placedFluid) || !FluidCompatApi.service().isFinite(placedFluid)
                    || !EnderIoThinLayer.isPlainFluidBlock(level, pos)) {
                return 0;
            }
            return Math.max(0, state.getAmount());
        } catch (RuntimeException | LinkageError error) {
            FluidCompatLog.errorThrottled("enderio-niard-refund", "Ender IO Niard refund check failed", error);
            return 0;
        }
    }
}
