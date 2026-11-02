package me.thanhtnt2.fluidcompact.compat;

import java.util.List;

import me.thanhtnt2.fluidcompact.api.compat.CompatModule;
import me.thanhtnt2.fluidcompact.api.compat.CompatModuleRegistry;
import me.thanhtnt2.fluidcompact.api.compat.CompatStatus;
import me.thanhtnt2.fluidcompact.api.event.FluidIntakeEvent;
import me.thanhtnt2.fluidcompact.compat.alexscaves.AlexsCavesCompatModule;
import me.thanhtnt2.fluidcompact.compat.immersiveengineering.ImmersiveEngineeringCompatModule;
import me.thanhtnt2.fluidcompact.compat.create.CreateCompatModule;
import me.thanhtnt2.fluidcompact.compat.fluidlogged.FluidloggedCompatModule;
import me.thanhtnt2.fluidcompact.compat.flowingfluids.FlowingFluidsPipesCompatModule;
import me.thanhtnt2.fluidcompact.compat.info.InfoCompatModule;
import me.thanhtnt2.fluidcompact.compat.mekanism.MekanismCompatModule;
import me.thanhtnt2.fluidcompact.compat.mekanismdieselgenerators.MekanismDieselGeneratorsCompatModule;
import me.thanhtnt2.fluidcompact.compat.modernindustrialization.ModernIndustrializationCompatModule;
import me.thanhtnt2.fluidcompact.compat.pneumaticcraft.PneumaticCraftCompatModule;
import me.thanhtnt2.fluidcompact.compat.mekanismelements.MekanismElementsCompatModule;
import me.thanhtnt2.fluidcompact.config.FluidCompactCompatConfig;
import me.thanhtnt2.fluidcompact.logging.DebugSnapshot;
import me.thanhtnt2.fluidcompact.logging.FluidCompactLog;
import me.thanhtnt2.fluidcompact.logging.IntakeAuditLogger;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Registers and starts every compat module. Kept separate from the core bootstrap so adding a mod
 * never means editing the code of an earlier integration.
 */
public final class CompatBootstrap {

    private static final List<String> MODS_TO_REPORT = List.of(
            "neoforge", "flowing_fluids", "modern_industrialization", "mekanism", "mekanismgenerators",
            "mekanismelements", "mekanismdieselgenerators", "mekmm", "alexscaves", "citadel", "immersiveengineering", "immersivepetroleum", "create", "pneumaticcraft", "pnc_ocean_addons", "pnc_electrostatic_fix", "fluidlogged", "jade", "jei", "yet_another_config_lib_v3");

    private CompatBootstrap() {
    }

    /** Registers the built-in modules. Called from the mod constructor so addons can register theirs too. */
    public static void registerBuiltInModules() {
        // Already wired elsewhere (backend, Jade, JEI); listed here for the config tab and the log only
        CompatModuleRegistry.register(new InfoCompatModule("flowing_fluids", "flowing_fluids", "Flowing Fluids (drain backend)"));
        CompatModuleRegistry.register(new InfoCompatModule("jade", "jade", "Jade (pump tooltip)"));
        CompatModuleRegistry.register(new InfoCompatModule("jei", "jei", "Just Enough Items (pump info page)"));
        // New isolated modules
        CompatModuleRegistry.register(new ModernIndustrializationCompatModule());
        CompatModuleRegistry.register(new MekanismCompatModule());
        CompatModuleRegistry.register(new MekanismElementsCompatModule());
        CompatModuleRegistry.register(new PneumaticCraftCompatModule());
        CompatModuleRegistry.register(new AlexsCavesCompatModule());
        CompatModuleRegistry.register(new ImmersiveEngineeringCompatModule());
        // Immersive Petroleum needs no hook: its pumpjacks draw from underground reservoirs, never from world fluid blocks
        CompatModuleRegistry.register(new InfoCompatModule("immersivepetroleum", "immersivepetroleum", "Immersive Petroleum (no hook needed)"));
        CompatModuleRegistry.register(new MekanismDieselGeneratorsCompatModule());
        CompatModuleRegistry.register(new CreateCompatModule());
        CompatModuleRegistry.register(new FluidloggedCompatModule());
        CompatModuleRegistry.register(new FlowingFluidsPipesCompatModule());
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // Queued after the core bootstrap, so the API is already bound when modules start
        event.enqueueWork(CompatBootstrap::initialize);
    }

    private static void initialize() {
        reportEnvironment();
        if (FluidCompactLog.isDebugMode()) {
            DebugSnapshot.logAll();
        }
        for (CompatModule module : CompatModuleRegistry.snapshot()) {
            activate(module);
        }
        NeoForge.EVENT_BUS.addListener(IntakeAuditLogger::onIntake);
        FluidCompactLog.info("Compat setup finished: {}", summary());
    }

    private static void activate(CompatModule module) {
        if (!ModList.get().isLoaded(module.modId())) {
            CompatModuleRegistry.updateStatus(module.id(), CompatStatus.MISSING_MOD);
            return;
        }
        if (!FluidCompactCompatConfig.COMPAT_ENABLED.get()) {
            CompatModuleRegistry.updateStatus(module.id(), CompatStatus.DISABLED);
            return;
        }
        try {
            module.initialize();
            CompatModuleRegistry.updateStatus(module.id(), CompatStatus.ACTIVE);
        } catch (RuntimeException | LinkageError error) {
            // One broken module must never take the others, or the game, down with it
            CompatModuleRegistry.updateStatus(module.id(), CompatStatus.FAILED);
            FluidCompactLog.error("Compat module '" + module.id() + "' failed to initialise", error);
        }
    }

    private static void reportEnvironment() {
        StringBuilder mods = new StringBuilder();
        for (String modId : MODS_TO_REPORT) {
            ModList.get().getModContainerById(modId).ifPresent(container ->
                    mods.append(modId).append('=').append(container.getModInfo().getVersion()).append(' '));
        }
        FluidCompactLog.info("Relevant mods: {}", mods.toString().trim());
    }

    private static String summary() {
        StringBuilder text = new StringBuilder();
        for (CompatModule module : CompatModuleRegistry.snapshot()) {
            text.append(module.id()).append('=').append(CompatModuleRegistry.statusOf(module.id())).append(' ');
        }
        return text.toString().trim();
    }
}
