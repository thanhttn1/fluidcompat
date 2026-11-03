package me.thanhtnt2.fluidcompat.compat.immersiveengineering.guard;

/** Remembers why the Immersive Engineering compat was switched off, so the module can say so in the log file. */
public final class PumpGuardStatus {

    private static volatile String disabledReason;

    private PumpGuardStatus() {
    }

    public static void disable(String reason) {
        disabledReason = reason;
    }

    /** @return why the compat is off, or null when it is on (or has not been checked yet) */
    public static String disabledReason() {
        return disabledReason;
    }
}
