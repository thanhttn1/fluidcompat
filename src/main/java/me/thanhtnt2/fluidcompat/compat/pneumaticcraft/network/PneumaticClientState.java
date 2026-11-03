package me.thanhtnt2.fluidcompat.compat.pneumaticcraft.network;

import net.minecraft.core.BlockPos;

/**
 * The last switch state the server reported to this client. One position and one boolean, replaced on every
 * update, so it cannot grow. Plain data: safe to load on a dedicated server.
 */
public final class PneumaticClientState {

    private static volatile long knownPos = Long.MIN_VALUE;
    private static volatile boolean knownEnabled = true;

    private PneumaticClientState() {
    }

    public static void update(BlockPos pos, boolean enabled) {
        knownPos = pos.asLong();
        knownEnabled = enabled;
    }

    /** Defaults to "on", which is also the default of a hopper that was never touched. */
    public static boolean isEnabled(BlockPos pos) {
        return knownPos != pos.asLong() || knownEnabled;
    }
}
