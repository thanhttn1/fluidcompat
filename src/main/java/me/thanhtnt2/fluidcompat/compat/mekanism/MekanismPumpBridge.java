package me.thanhtnt2.fluidcompat.compat.mekanism;

import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import me.thanhtnt2.fluidcompat.compat.common.CompatGate;
import me.thanhtnt2.fluidcompat.api.FluidCompatTags;
import me.thanhtnt2.fluidcompat.config.FluidCompatModulesConfig;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * Decision logic for the Mekanism Electric Pump hook. Has no reference to Mekanism classes.
 *
 * <p>Mekanism already removes source blocks itself when its internal {@code shouldPump} says so, and
 * skips removal for "infinite" fluids. We only flip that answer for fluids Fluid Compat manages,
 * so the world is drained by Mekanism's own, well tested pickup code.
 */
public final class MekanismPumpBridge {

    private MekanismPumpBridge() {
    }

    /** @return true when the pump must remove the source block it just sucked from */
    public static boolean shouldForceSourceRemoval(Level level, Fluid fluid) {
        if (level.isClientSide() || !FluidCompatApi.isReady()) {
            return false;
        }
        try {
            return isForced(fluid);
        } catch (RuntimeException | LinkageError error) {
            // Fail open: a broken hook must never stop Mekanism's pump from working
            FluidCompatLog.errorThrottled("mekanism-electric-pump",
                    "Mekanism electric pump hook failed for fluid " + BuiltInRegistries.FLUID.getKey(fluid), error);
            return false;
        }
    }

    private static boolean isForced(Fluid fluid) {
        if (!CompatGate.masterEnabled() || !FluidCompatModulesConfig.ELECTRIC_PUMP_ENABLED.get()) {
            return false;
        }
        if (!FluidCompatApi.service().isFinite(fluid)) {
            return false;
        }
        if (fluid.is(FluidTags.WATER)) {
            return FluidCompatModulesConfig.ELECTRIC_PUMP_WATER.get();
        }
        if (fluid.is(FluidTags.LAVA)) {
            return FluidCompatModulesConfig.ELECTRIC_PUMP_LAVA.get();
        }
        return fluid.is(FluidCompatTags.MANAGED_FLUIDS) && FluidCompatModulesConfig.ELECTRIC_PUMP_TAGGED_FLUIDS.get();
    }
}
