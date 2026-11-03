package me.thanhtnt2.fluidcompat.compat.mekanismelements;

import me.thanhtnt2.fluidcompat.api.compat.CompatModule;
import me.thanhtnt2.fluidcompat.compat.CompatMixinState;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;

/** Mekanism Elements: the Seawater Pump drains the water source below it. The hook is a mixin, see {@code mixin.impl}. */
public final class MekanismElementsCompatModule implements CompatModule {

    public static final String ID = "mekanism_elements";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String modId() {
        return "mekanismelements";
    }

    @Override
    public String displayName() {
        return "Mekanism Elements (Seawater Pump)";
    }

    @Override
    public void initialize() {
        if (CompatMixinState.isApplied(ID)) {
            FluidCompatLog.info("[mekanism_elements] Seawater Pump hook active");
            return;
        }
        FluidCompatLog.warn("[mekanism_elements] Seawater Pump hook is NOT applied; the pump keeps its original behaviour");
    }
}
