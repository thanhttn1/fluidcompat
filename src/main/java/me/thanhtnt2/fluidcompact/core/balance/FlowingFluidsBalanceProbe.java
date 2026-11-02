package me.thanhtnt2.fluidcompact.core.balance;

import java.util.OptionalDouble;

import me.thanhtnt2.fluidcompact.api.FluidCompactApi;
import traben.flowing_fluids.api.FlowingFluidsAPI;
import traben.flowing_fluids.config.FFConfig;

/** Reads (never writes) Flowing Fluids' own tick-time target so both mods agree on when the server is slow. */
final class FlowingFluidsBalanceProbe {

    private static final double MILLIS_PER_TICK_AT_FULL_SPEED = 50.0D;

    private FlowingFluidsBalanceProbe() {
    }

    static OptionalDouble targetMillis() {
        try {
            FFConfig flowingFluidsConfig = FlowingFluidsAPI.getInstance(FluidCompactApi.MOD_ID).getConfig();
            if (flowingFluidsConfig.autoPerformanceMode == FFConfig.AutoPerformance.OFF) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(MILLIS_PER_TICK_AT_FULL_SPEED * flowingFluidsConfig.autoPerformanceMSPTargetMultiplier);
        } catch (RuntimeException | LinkageError unavailable) {
            // Flowing Fluids changed or is not ready yet: fall back to FluidCompact's own target
            return OptionalDouble.empty();
        }
    }
}
