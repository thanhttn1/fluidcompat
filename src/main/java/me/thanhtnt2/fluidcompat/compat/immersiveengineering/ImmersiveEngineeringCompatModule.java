package me.thanhtnt2.fluidcompat.compat.immersiveengineering;

import me.thanhtnt2.fluidcompat.api.compat.CompatModule;
import me.thanhtnt2.fluidcompat.compat.CompatMixinState;
import me.thanhtnt2.fluidcompat.compat.immersiveengineering.guard.PumpGuardStatus;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;

/**
 * Immersive Engineering. Only the Fluid Pump takes fluid out of the world. With Flowing Fluids it is stopped from
 * making endless water, empties a pond completely including its thin layers, and no longer leaves cobblestone behind.
 * The Fluid Placer needs nothing, it puts down one source block per 1000 mB taken from its tank. The hooks are mixins
 * that are applied all together or not at all, depending on {@code guard.PumpContract}.
 */
public final class ImmersiveEngineeringCompatModule implements CompatModule {

    public static final String ID = "immersiveengineering";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String modId() {
        return "immersiveengineering";
    }

    @Override
    public String displayName() {
        return "Immersive Engineering (Fluid Pump)";
    }

    @Override
    public void initialize() {
        if (CompatMixinState.isApplied(ID)) {
            FluidCompatLog.info("[immersiveengineering] Fluid Pump hooks active (pump contract verified)");
            return;
        }
        String reason = PumpGuardStatus.disabledReason();
        FluidCompatLog.warn("[immersiveengineering] Fluid Pump hooks are NOT applied; the pump keeps making endless water "
                + "from two-source pools{}", reason == null ? "" : ". Reason: " + reason);
    }
}
