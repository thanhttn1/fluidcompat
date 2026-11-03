package me.thanhtnt2.fluidcompat.compat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Remembers whether each compat mixin was applied, so the module can report it in the log file later. */
public final class CompatMixinState {

    private static final Map<String, Boolean> APPLIED = new ConcurrentHashMap<>();

    private CompatMixinState() {
    }

    public static void record(String moduleId, boolean applied) {
        APPLIED.put(moduleId, applied);
    }

    public static boolean isApplied(String moduleId) {
        return APPLIED.getOrDefault(moduleId, false);
    }
}
