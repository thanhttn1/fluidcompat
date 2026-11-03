package me.thanhtnt2.fluidcompat.compat.immersiveengineering;

import java.util.concurrent.atomic.AtomicInteger;

import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;

/**
 * Circuit breaker shared by all Immersive Engineering pump hooks.
 *
 * <p>The load-time guard ({@code PumpContract}) catches an Immersive Engineering update that changed the pump's
 * code. This catches the other kind of break, one that only shows while the game runs (for example a changed
 * Flowing Fluids API): after a few errors every hook switches itself off for the session and the pump goes back to
 * its original behaviour.
 */
public final class PumpHookHealth {

    // After this many errors the hooks stop instead of failing on every pump operation
    private static final int MAX_FAILURES = 5;
    private static final AtomicInteger FAILURES = new AtomicInteger();
    private static volatile boolean tripped;

    private PumpHookHealth() {
    }

    public static boolean isTripped() {
        return tripped;
    }

    public static void recordFailure(Throwable error) {
        FluidCompatLog.errorThrottled("immersiveengineering-pump", "Immersive Engineering pump hook failed", error);
        if (FAILURES.incrementAndGet() >= MAX_FAILURES && !tripped) {
            tripped = true;
            FluidCompatLog.warn("[immersiveengineering] Fluid Pump hooks switched off for this session after repeated errors; "
                    + "the pump is back to its original behaviour. Update Fluid Compat.");
        }
    }
}
