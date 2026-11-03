package me.thanhtnt2.fluidcompat.compat.pneumaticcraft;

import me.thanhtnt2.fluidcompat.api.compat.CompatModule;
import me.thanhtnt2.fluidcompat.compat.CompatMixinState;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;

/**
 * PneumaticCraft: Repressurized. Liquid Hopper and Gas Lift stop duplicating water.
 * The hook is a mixin, see {@code mixin.impl}; the rules live in {@link PneumaticPickupBridge}.
 */
public final class PneumaticCraftCompatModule implements CompatModule {

    public static final String ID = "pneumaticcraft";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String modId() {
        return "pneumaticcraft";
    }

    @Override
    public String displayName() {
        return "PneumaticCraft: Repressurized (Liquid Hopper, Gas Lift)";
    }

    @Override
    public void initialize() {
        logHook(ID, "pickup hook (Liquid Hopper, Gas Lift: no infinite water)");
        logHook("pneumaticcraft_hopper", "Liquid Hopper leftover-water pull");
    }

    private static void logHook(String stateKey, String description) {
        if (CompatMixinState.isApplied(stateKey)) {
            FluidCompatLog.info("[pneumaticcraft] {} active", description);
            return;
        }
        FluidCompatLog.warn("[pneumaticcraft] {} is NOT applied; PneumaticCraft keeps its own behaviour there", description);
    }
}
