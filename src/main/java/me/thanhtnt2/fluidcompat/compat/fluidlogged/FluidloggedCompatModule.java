package me.thanhtnt2.fluidcompat.compat.fluidlogged;

import me.thanhtnt2.fluidcompat.api.compat.CompatModule;
import me.thanhtnt2.fluidcompat.config.FluidCompatModulesConfig;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;

/** Fluidlogged: lets water and other fluids enter and flow through pipes and cables instead of breaking or blocking. */
public final class FluidloggedCompatModule implements CompatModule {

    public static final String ID = "fluidlogged";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String modId() {
        return "fluidlogged";
    }

    @Override
    public String displayName() {
        return "Fluidlogged (fluid passes through pipes)";
    }

    @Override
    public void initialize() {
        if (!FluidCompatModulesConfig.FLUIDLOGGED_ENABLED.get()) {
            FluidCompatLog.info("[fluidlogged] disabled in config, no addon registered");
            return;
        }
        if (!FluidCompatModulesConfig.FLUIDLOGGED_ACCEPT_DUPLICATION_RISK.get()) {
            FluidCompatLog.info("[fluidlogged] addon NOT registered: it can duplicate water together with Flowing Fluids "
                    + "(set fluidlogged_pipes.acceptDuplicationRisk=true to force it)");
            return;
        }
        FluidloggedAddonRegistrar.register();
    }
}
