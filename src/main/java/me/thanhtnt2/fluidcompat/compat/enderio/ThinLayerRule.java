package me.thanhtnt2.fluidcompat.compat.enderio;

/** The number rules of the Ender IO hooks, kept free of Minecraft types so they can be unit tested. */
public final class ThinLayerRule {

    private static final int MILLIBUCKETS_PER_LEVEL = 125;
    private static final int BUCKET = 1000;

    private ThinLayerRule() {
    }

    /** A flowing cell counts from its first level; an empty cell (0) never does. */
    public static boolean isUsableLayer(int levels) {
        return levels >= 1;
    }

    /**
     * What the Niard may still be charged for a bucket it places over a layer that already held {@code heldLevels}:
     * the whole bucket minus the part that was already in the world, never less than one level.
     */
    public static int niardChargeMillibuckets(int heldLevels) {
        int held = Math.max(0, Math.min(7, heldLevels));
        return Math.max(MILLIBUCKETS_PER_LEVEL, BUCKET - held * MILLIBUCKETS_PER_LEVEL);
    }
}
