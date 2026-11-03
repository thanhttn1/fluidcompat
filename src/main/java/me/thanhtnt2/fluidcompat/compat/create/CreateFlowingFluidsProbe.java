package me.thanhtnt2.fluidcompat.compat.create;

import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;
import traben.flowing_fluids.api.FlowingFluidsAPI;
import traben.flowing_fluids.config.FFConfig;

/** Reads Flowing Fluids' Create settings (read only) so the startup report can spot a mismatch with Fluid Compat. */
final class CreateFlowingFluidsProbe {

    private CreateFlowingFluidsProbe() {
    }

    static void report(boolean waterIsFinite) {
        try {
            FFConfig config = FlowingFluidsAPI.getInstance(FluidCompatApi.MOD_ID).getConfig();
            FluidCompatLog.info("[create] Flowing Fluids Create settings: waterWheelMode={} infinitePipes={} waterWheelFlowMaxTickInterval={}",
                    config.create_waterWheelMode, config.create_infinitePipes, config.create_waterWheelFlowMaxTickInterval);
            if (waterIsFinite && config.create_infinitePipes) {
                // Not changed on purpose: it is the pack maker's choice, but it is worth knowing about
                FluidCompatLog.warn("[create] Fluid Compat treats water as finite but Flowing Fluids has create_infinitePipes=true, "
                        + "so Create's open pipe ends still make infinite water. Set it to false in flowing_fluids.json for consistent finite water.");
            }
        } catch (RuntimeException | LinkageError unavailable) {
            FluidCompatLog.warn("[create] could not read Flowing Fluids' Create settings: {}", unavailable.toString());
        }
    }
}
