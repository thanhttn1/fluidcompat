package me.thanhtnt2.fluidcompat.compat.mekanismelements;

import me.thanhtnt2.fluidcompat.api.compat.CompatModule;
import me.thanhtnt2.fluidcompat.compat.CompatMixinState;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;

/** Mekanism Elements: the Seawater Pump drains the water source below it. The hook is a mixin, see {@code mixin.impl}. */
public final class MekanismElementsCompatModule implements CompatModule {

    public static final String ID = "mekanism_elements";
    public static final String THIN_WATER_ID = "mekanism_elements_thin_water";
    private static final String PUMP_CLASS = "fixdol.mekanismelements.common.tile.machine.TileEntitySeawaterPump";

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
        // Loading the pump class (without running anything in it) makes Mixin ask the guards now, so the verdicts
        // below are known at report time instead of being "never asked" for a block entity class nobody touched yet
        loadPumpClass();
        reportChargeHook();
        reportThinWaterHook();
    }

    private static void loadPumpClass() {
        try {
            Class.forName(PUMP_CLASS, false, MekanismElementsCompatModule.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError error) {
            FluidCompatLog.warn("[mekanism_elements] could not load the Seawater Pump class: " + error);
        }
    }

    // "allowed" only says the guard accepted the code; SeawaterPumpBridge logs "reached" when the running game really calls it
    private static void reportChargeHook() {
        if (CompatMixinState.isApplied(ID)) {
            FluidCompatLog.info("[mekanism_elements] Seawater Pump hook allowed (a 'reached for the first time' line follows once a pump cycle runs)");
            return;
        }
        FluidCompatLog.warn("[mekanism_elements] Seawater Pump hook is NOT applied; the pump keeps its original behaviour");
    }

    private static void reportThinWaterHook() {
        if (CompatMixinState.isApplied(THIN_WATER_ID)) {
            FluidCompatLog.info("[mekanism_elements] Seawater Pump thin water hook allowed");
        } else if (CompatMixinState.hasVerdict(THIN_WATER_ID)) {
            FluidCompatLog.warn("[mekanism_elements] Seawater Pump thin water hook NOT allowed (see 'guard refused the pump' in latest.log); "
                    + "the pump stops as soon as the water below it is no longer a full source");
        } else {
            FluidCompatLog.warn("[mekanism_elements] Seawater Pump thin water hook was never asked (its mixin config did not load)");
        }
    }
}
