package me.thanhtnt2.fluidcompact.compat.mekanismdieselgenerators;

import me.thanhtnt2.fluidcompact.api.compat.CompatModule;
import me.thanhtnt2.fluidcompact.compat.CompatMixinState;
import me.thanhtnt2.fluidcompact.compat.mekanismdieselgenerators.mixin.MekanismDieselGeneratorsMixinPlugin;
import me.thanhtnt2.fluidcompact.config.FluidCompactCompatConfig;
import me.thanhtnt2.fluidcompact.logging.FluidCompactLog;
import net.neoforged.fml.ModList;

/**
 * Mekanism Diesel Generators. Its only machines that take water from the world are the Shaft Pump and the Inline Pump,
 * which exist when Create is installed: they drain the world instead of making water. The Oil Pumpjack draws from
 * underground deposits, and the cooling tower, digester and pipes only handle fluid inside their own tanks.
 * The hook is a mixin, see {@code mixin.impl}; the rules live in {@link MdgPumpBridge}.
 */
public final class MekanismDieselGeneratorsCompatModule implements CompatModule {

    public static final String ID = MekanismDieselGeneratorsMixinPlugin.MODULE_ID;
    private static final String PUMP_CLASS = "com.jambonbeurre78.mdg.compat.create.AuxDriveBlockEntity";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String modId() {
        return "mekanismdieselgenerators";
    }

    @Override
    public String displayName() {
        return "Mekanism Diesel Generators (Shaft Pump, Inline Pump)";
    }

    @Override
    public void initialize() {
        if (!ModList.get().isLoaded("create")) {
            FluidCompactLog.info("[mekanism_diesel_generators] Create is not installed, so there are no pumps that take water from the world");
            return;
        }
        if (!FluidCompactCompatConfig.MDG_PUMPS_ENABLED.get()) {
            FluidCompactLog.info("[mekanism_diesel_generators] pump hook disabled in config");
            return;
        }
        ensurePumpClassIsLoaded();
        if (CompatMixinState.isApplied(ID)) {
            FluidCompactLog.info("[mekanism_diesel_generators] Shaft Pump and Inline Pump hook active");
            return;
        }
        FluidCompactLog.warn("[mekanism_diesel_generators] pump hook is NOT applied; the Shaft Pump and Inline Pump keep making endless water");
    }

    // The mixin plugin is only asked when the pump class is loaded. Loading it here (without running any of its code)
    // makes sure the verdict is known before it is reported, whatever order the other mod registered things in.
    private static void ensurePumpClassIsLoaded() {
        try {
            Class.forName(PUMP_CLASS, false, MekanismDieselGeneratorsCompatModule.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError error) {
            FluidCompactLog.warn("[mekanism_diesel_generators] could not load the pump class: {}", error.toString());
        }
    }
}
