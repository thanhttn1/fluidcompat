package me.thanhtnt2.fluidcompat.compat.mekanismelements;

import java.util.List;

import me.thanhtnt2.fluidcompat.FluidCompat;
import me.thanhtnt2.fluidcompat.api.FiniteFluidService;
import me.thanhtnt2.fluidcompat.compat.CompatMixinState;
import me.thanhtnt2.fluidcompat.compat.common.CompatGate;
import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import me.thanhtnt2.fluidcompat.api.FluidIntakeRequest;
import me.thanhtnt2.fluidcompat.api.FluidIntakeResult;
import me.thanhtnt2.fluidcompat.config.FluidCompatModulesConfig;
import me.thanhtnt2.fluidcompat.config.ShortfallBehavior;
import me.thanhtnt2.fluidcompat.core.FluidCompatAttachments;
import me.thanhtnt2.fluidcompat.core.PumpStatistics;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Charges the world for the seawater a Mekanism Elements Seawater Pump makes.
 * Called from the mixin right before the pump inserts its product into its tank.
 * Has no reference to Mekanism or Mekanism Elements classes.
 */
public final class SeawaterPumpBridge {

    // The pump only works with a water source directly below it
    private static final Fluid CONSUMED_FLUID = Fluids.WATER;
    private static final String HOOK_ID = "mekanism_elements.charge";

    private SeawaterPumpBridge() {
    }

    /**
     * @param producedSeawater what the pump is about to insert into its tank
     * @return what it may insert instead (possibly empty); the original stack is never modified
     */
    public static FluidStack chargeWorld(BlockEntity pump, FluidStack producedSeawater) {
        try {
            return charge(pump, producedSeawater);
        } catch (RuntimeException | LinkageError error) {
            // Fail open: a broken hook must never stop the pump from working
            FluidCompatLog.errorThrottled("seawater-pump",
                    "Seawater pump hook failed at " + pump.getBlockPos().toShortString(), error);
            return producedSeawater;
        }
    }

    private static FluidStack charge(BlockEntity pump, FluidStack producedSeawater) {
        if (!(pump.getLevel() instanceof ServerLevel level) || producedSeawater.isEmpty() || !FluidCompatApi.isReady()) {
            return producedSeawater;
        }
        if (!CompatGate.masterEnabled() || !FluidCompatModulesConfig.SEAWATER_PUMP_ENABLED.get()) {
            return producedSeawater;
        }
        FiniteFluidService service = FluidCompatApi.service();
        if (!service.isFinite(CONSUMED_FLUID)) {
            return producedSeawater;
        }

        if (CompatMixinState.markHookReached(HOOK_ID)) {
            FluidCompatLog.info("[mekanism_elements] Seawater Pump hook reached for the first time at " + pump.getBlockPos().toShortString());
        }
        long gameTime = level.getGameTime();
        PumpStatistics statistics = pump.getData(FluidCompatAttachments.PUMP_STATISTICS.get());
        int minTicks = FluidCompatModulesConfig.SEAWATER_MIN_TICKS_BETWEEN_INTAKES.get();
        if (statistics.isCoolingDown(gameTime, minTicks)) {
            return FluidStack.EMPTY;
        }

        FluidIntakeResult result = service.requestIntake(buildRequest(level, pump, producedSeawater));
        if (!result.managed() || result.requestedLevels() <= 0) {
            return producedSeawater;
        }

        pump.setData(FluidCompatAttachments.PUMP_STATISTICS.get(),
                statistics.withIntake(result.drainedLevels(), result.isFullyServed(), gameTime));
        pump.setChanged();
        return applyShortfall(producedSeawater, result);
    }

    private static FluidIntakeRequest buildRequest(ServerLevel level, BlockEntity pump, FluidStack producedSeawater) {
        int producedLevels = Math.ceilDiv(producedSeawater.getAmount(), FluidCompatApi.MILLIBUCKETS_PER_LEVEL);
        int percent = FluidCompatModulesConfig.SEAWATER_DRAIN_PERCENT.get();
        int levelsToDrain = Math.ceilDiv(producedLevels * percent, 100);
        BlockPos pumpPos = pump.getBlockPos();
        List<BlockPos> sources = List.of(pumpPos.below());
        return new FluidIntakeRequest(level, pumpPos, machineIdOf(pump), CONSUMED_FLUID, levelsToDrain, sources);
    }

    private static FluidStack applyShortfall(FluidStack producedSeawater, FluidIntakeResult result) {
        if (result.isFullyServed()) {
            return producedSeawater;
        }
        if (FluidCompatModulesConfig.SEAWATER_SHORTFALL.get() == ShortfallBehavior.STOP || result.drainedLevels() <= 0) {
            return FluidStack.EMPTY;
        }
        // Scale down proportionally so a half-paid cycle yields half the seawater
        long scaledAmount = (long) producedSeawater.getAmount() * result.drainedLevels() / result.requestedLevels();
        return scaledAmount <= 0L ? FluidStack.EMPTY : producedSeawater.copyWithAmount((int) scaledAmount);
    }

    private static ResourceLocation machineIdOf(BlockEntity pump) {
        ResourceLocation key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(pump.getType());
        return key != null ? key : ResourceLocation.fromNamespaceAndPath(FluidCompat.MODID, "unknown_seawater_pump");
    }
}
