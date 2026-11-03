package me.thanhtnt2.fluidcompat.compat.modernindustrialization;

import me.thanhtnt2.fluidcompat.api.compat.CompatModule;
import me.thanhtnt2.fluidcompat.compat.CompatMixinState;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;

/**
 * Modern Industrialization: Bronze, Steel and Electric Water Pumps drain the world instead of creating water.
 * The hook is a mixin, see {@code mixin.impl}; the rules live in {@link MiPumpBridge}.
 */
public final class ModernIndustrializationCompatModule implements CompatModule {

    public static final String ID = "modern_industrialization";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String modId() {
        return "modern_industrialization";
    }

    @Override
    public String displayName() {
        return "Modern Industrialization (Water Pumps)";
    }

    @Override
    public void initialize() {
        if (CompatMixinState.isApplied(ID)) {
            FluidCompatLog.info("[modern_industrialization] Water Pump hook active");
            return;
        }
        FluidCompatLog.warn("[modern_industrialization] Water Pump hook is NOT applied; pumps keep vanilla MI behaviour");
    }
}
