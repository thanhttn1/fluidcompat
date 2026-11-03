package me.thanhtnt2.fluidcompat.compat.create;

import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import me.thanhtnt2.fluidcompat.api.compat.CompatModule;
import me.thanhtnt2.fluidcompat.compat.common.ExternalHandledBlocks;
import me.thanhtnt2.fluidcompat.config.FluidCompatModulesConfig;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;

/**
 * Create. Flowing Fluids already supports Create through its own config, so this module adds no hooks and no mixins:
 * it only checks the combination, keeps Fluid Compat's pipe features away from Create's blocks, and reports to the log.
 */
public final class CreateCompatModule implements CompatModule {

    public static final String ID = "create";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String modId() {
        return "create";
    }

    @Override
    public String displayName() {
        return "Create (handled by Flowing Fluids)";
    }

    @Override
    public void initialize() {
        if (!FluidCompatModulesConfig.CREATE_ENABLED.get()) {
            FluidCompatLog.info("[create] check disabled in config");
            return;
        }
        FluidCompatLog.info("[create] Create {} with Flowing Fluids {}: Fluid Compat adds no Create hooks",
                versionOf("create"), versionOf("flowing_fluids"));
        FluidCompatLog.info("[create] Create blocks kept out of Fluid Compat pipe features: {}", ExternalHandledBlocks.createExclusionActive());
        boolean waterIsFinite = FluidCompatApi.isReady() && FluidCompatApi.service().isFinite(Fluids.WATER);
        CreateFlowingFluidsProbe.report(waterIsFinite);
    }

    private static String versionOf(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("?");
    }
}
