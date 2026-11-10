package me.thanhtnt2.fluidcompat.compat.mekanismdieselgenerators;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import me.thanhtnt2.fluidcompat.FluidCompat;
import me.thanhtnt2.fluidcompat.api.FiniteFluidService;
import me.thanhtnt2.fluidcompat.compat.common.CompatGate;
import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import me.thanhtnt2.fluidcompat.api.FluidIntakeRequest;
import me.thanhtnt2.fluidcompat.api.FluidIntakeResult;
import me.thanhtnt2.fluidcompat.config.FluidCompatModulesConfig;
import me.thanhtnt2.fluidcompat.config.FluidCompatConfig;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Makes the world pay for the water a Mekanism Diesel Generators Shaft Pump or Inline Pump puts into its tank.
 *
 * <p>Called from the pump mixin right before the tank is filled, so the amount in {@code requested} is exactly what the
 * pump gains and exactly what must leave the world. These pumps work every tick, in amounts that are rarely a whole
 * number of levels (a level is 125 mB), so the part of a paid level the pump has not used yet is remembered per pump.
 * That keeps the accounting exact: the world is never charged for water nobody received. This class has no
 * compile-time reference to Mekanism Diesel Generators.
 */
public final class MdgPumpBridge {

    // Same idea as the other pump hooks: after this many failures in a row the hook stops for the session,
    // instead of throwing every tick. The pump then runs like in plain Mekanism Diesel Generators.
    private static final int MAX_CONSECUTIVE_FAILURES = 5;

    // Weak keys: a pump that is broken or unloaded takes its remainder with it, which only ever costs the world (never gains)
    private static final Map<BlockEntity, PaidWater> PAID_BUT_UNUSED = Collections.synchronizedMap(new WeakHashMap<>());

    // One level is 125 mB at 100% drain; the percent setting scales how much water a level is worth (125 * 100 = 12500)
    private static final long MILLIBUCKETS_PER_PERCENT_UNIT = (long) FluidCompatApi.MILLIBUCKETS_PER_LEVEL * 100L;

    private static int consecutiveFailures;
    private static boolean switchedOff;

    private MdgPumpBridge() {
    }

    /**
     * Forgets the failure counter, the switched-off state and the per-pump remainders. The integrated server is stopped
     * and started again inside one game session, and a pump that tripped the breaker in one world must not stay
     * unlimited in the next one. A dropped remainder is water the world paid for and nobody received, so this can
     * only cost the world, never give a pump free water.
     */
    static void resetForNewServer() {
        if (switchedOff) {
            FluidCompatLog.info("[mekanism_diesel_generators] a new server started, the pump hook is switched on again");
        }
        consecutiveFailures = 0;
        switchedOff = false;
        PAID_BUT_UNUSED.clear();
    }

    /**
     * @param inletCell the world block the pump is drawing from, or {@code null} if the hook could not note it
     * @param requested what the pump is about to put into its tank
     * @return what the pump may put into its tank instead: the same stack when the world could pay for it,
     *         a smaller one (possibly {@link FluidStack#EMPTY}) when the water ran short
     */
    public static FluidStack payForDrawnWater(BlockEntity pump, BlockPos inletCell, FluidStack requested) {
        if (switchedOff || requested.isEmpty() || !FluidCompatApi.isReady()) {
            return requested;
        }
        try {
            FluidStack granted = payInsideLevel(pump, inletCell, requested);
            consecutiveFailures = 0;
            return granted;
        } catch (RuntimeException | LinkageError error) {
            // Fail open like every other bridge: a broken hook must never become a "ticking block entity" crash
            return failOpen(pump, requested, error);
        }
    }

    private static FluidStack failOpen(BlockEntity pump, FluidStack requested, Throwable error) {
        FluidCompatLog.errorThrottled("mdg-pump", "Mekanism Diesel Generators pump hook failed at "
                + pump.getBlockPos().toShortString() + "; the pump runs unlimited until this is fixed", error);
        consecutiveFailures++;
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            switchedOff = true;
            FluidCompatLog.error("Mekanism Diesel Generators pump hook failed " + MAX_CONSECUTIVE_FAILURES
                    + " times in a row and is switched off for this session", error);
        }
        return requested;
    }

    private static FluidStack payInsideLevel(BlockEntity pump, BlockPos inletCell, FluidStack requested) {
        if (!CompatGate.masterEnabled() || !FluidCompatModulesConfig.MDG_PUMPS_ENABLED.get()
                || !(pump.getLevel() instanceof ServerLevel level)) {
            return requested;
        }
        Fluid fluid = requested.getFluid();
        FiniteFluidService service = FluidCompatApi.service();
        if (!service.isFinite(fluid)) {
            return requested;
        }
        if (inletCell == null) {
            // Cannot happen while both hooks are applied together; if it does, say so instead of failing silently
            FluidCompatLog.errorThrottled("mdg-pump-inlet", "Mekanism Diesel Generators pump hook did not see the inlet block at "
                    + pump.getBlockPos().toShortString() + "; the pump runs unlimited", new IllegalStateException("inlet cell not noted"));
            return requested;
        }

        PaidWater paid = PAID_BUT_UNUSED.computeIfAbsent(pump, key -> new PaidWater());
        int wanted = requested.getAmount();
        if (paid.millibuckets >= wanted) {
            paid.millibuckets -= wanted;
            return requested;
        }

        int missing = wanted - paid.millibuckets;
        int drainPercent = FluidCompatConfig.DRAIN_PERCENT.get();
        // Round up so the world always pays at least what the pump is about to receive
        int levelsToDrain = (int) Math.ceilDiv((long) missing * drainPercent, MILLIBUCKETS_PER_PERCENT_UNIT);
        FluidIntakeRequest request = new FluidIntakeRequest(
                level, pump.getBlockPos(), machineIdOf(pump), fluid, levelsToDrain, List.of(inletCell));

        FluidIntakeResult result = service.requestIntake(request);
        if (!result.managed() || result.requestedLevels() <= 0) {
            return requested;
        }
        return settle(paid, requested, drainPercent, result);
    }

    /**
     * Turns what the world paid into water for the tank, and keeps the part the pump could not use yet for the next call.
     * Each level the world gives up is worth {@code 12500 / drainPercent} mB to the pump (125 mB at 100%), so a half-paid
     * request gives half the water and nothing is ever received that the world did not pay for.
     */
    private static FluidStack settle(PaidWater paid, FluidStack requested, int drainPercent, FluidIntakeResult result) {
        long paidMillibuckets = (long) result.drainedLevels() * MILLIBUCKETS_PER_PERCENT_UNIT / drainPercent;
        long available = paid.millibuckets + paidMillibuckets;
        long granted = Math.min(requested.getAmount(), available);
        paid.millibuckets = (int) (available - granted);

        if (granted >= requested.getAmount()) {
            return requested;
        }
        return granted <= 0L ? FluidStack.EMPTY : requested.copyWithAmount((int) granted);
    }

    private static ResourceLocation machineIdOf(BlockEntity pump) {
        ResourceLocation key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(pump.getType());
        return key != null ? key : ResourceLocation.fromNamespaceAndPath(FluidCompat.MODID, "unknown_pump");
    }

    /** Water the world already paid for but the pump has not received yet: less than one drained level's worth. */
    private static final class PaidWater {
        private int millibuckets;
    }
}
