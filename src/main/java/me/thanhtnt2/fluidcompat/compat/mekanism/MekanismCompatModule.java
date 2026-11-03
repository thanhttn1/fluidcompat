package me.thanhtnt2.fluidcompat.compat.mekanism;

import me.thanhtnt2.fluidcompat.api.compat.CompatModule;
import me.thanhtnt2.fluidcompat.compat.CompatMixinState;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;

/** Mekanism: Electric Pump stops duplicating water and lava. The hook itself is a mixin, see {@code mixin.impl}. */
public final class MekanismCompatModule implements CompatModule {

    public static final String ID = "mekanism";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String modId() {
        return "mekanism";
    }

    @Override
    public String displayName() {
        return "Mekanism (Electric Pump)";
    }

    @Override
    public void initialize() {
        if (CompatMixinState.isApplied(ID)) {
            FluidCompatLog.info("[mekanism] Electric Pump hook active");
            return;
        }
        FluidCompatLog.warn("[mekanism] Electric Pump hook is NOT applied; the pump keeps vanilla Mekanism behaviour");
    }
}
