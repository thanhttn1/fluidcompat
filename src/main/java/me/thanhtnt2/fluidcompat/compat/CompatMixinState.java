package me.thanhtnt2.fluidcompat.compat;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers what happened to each compat mixin, so the modules can report it in the log file.
 *
 * <p>Two different facts are kept apart on purpose. The <b>verdict</b> says the guard accepted (or refused) the target
 * code; it does not prove that Mixin then managed to inject. The <b>reached</b> mark is set by the hook itself the first
 * time the running game really calls it, which is the only proof that the hook is live.
 */
public final class CompatMixinState {

    private static final Map<String, Boolean> APPLIED = new ConcurrentHashMap<>();
    private static final Set<String> REACHED = ConcurrentHashMap.newKeySet();

    private CompatMixinState() {
    }

    public static void record(String moduleId, boolean applied) {
        APPLIED.put(moduleId, applied);
    }

    public static boolean isApplied(String moduleId) {
        return APPLIED.getOrDefault(moduleId, false);
    }

    /** Whether the guard has run for this id at all; false means its mixin config was never loaded. */
    public static boolean hasVerdict(String moduleId) {
        return APPLIED.containsKey(moduleId);
    }

    /** @return true only the first time this hook id is marked, so the caller can log "reached" exactly once */
    public static boolean markHookReached(String hookId) {
        return REACHED.add(hookId);
    }
}
