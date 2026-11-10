package me.thanhtnt2.fluidcompat.compat.mekanismelements;

/** The one number rule of the thin water hook, kept free of Minecraft types so it can be unit tested. */
public final class ThinWaterRule {

    private ThinWaterRule() {
    }

    /** A flowing water cell counts for the pump from its first level; an empty cell (0) never does. */
    public static boolean isUsableLayer(int levels) {
        return levels >= 1;
    }
}
