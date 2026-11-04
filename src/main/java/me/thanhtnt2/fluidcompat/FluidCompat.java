package me.thanhtnt2.fluidcompat;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import me.thanhtnt2.fluidcompat.compat.CompatBootstrap;
import me.thanhtnt2.fluidcompat.compat.pneumaticcraft.PneumaticAttachments;
import me.thanhtnt2.fluidcompat.compat.pneumaticcraft.network.PneumaticNetwork;
import me.thanhtnt2.fluidcompat.config.FluidCompatModulesConfig;
import me.thanhtnt2.fluidcompat.config.FluidCompatConfig;
import me.thanhtnt2.fluidcompat.config.LegacyConfigMigration;
import me.thanhtnt2.fluidcompat.core.FluidCompatAttachments;
import me.thanhtnt2.fluidcompat.core.FluidCompatBootstrap;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/** Common entry point. Contains no client code, so it is safe on dedicated servers. */
@Mod(FluidCompat.MODID)
public final class FluidCompat {

    public static final String MODID = "fluidcompat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FluidCompat(IEventBus modEventBus, ModContainer modContainer) {
        // Must come first: the configs are read from disk right after the constructors have run
        LegacyConfigMigration.copyOldSettingsIfNeeded();
        FluidCompatAttachments.ATTACHMENT_TYPES.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, FluidCompatConfig.SPEC);
        modEventBus.addListener(this::onCommonSetup);
        registerCompatLayer(modEventBus, modContainer);
    }

    // Compat modules live in their own packages and config file; this is their single entry point
    private static void registerCompatLayer(IEventBus modEventBus, ModContainer modContainer) {
        FluidCompatLog.initialize();
        modContainer.registerConfig(ModConfig.Type.COMMON, FluidCompatModulesConfig.SPEC, "fluidcompat-compat.toml");
        // Both are free of PneumaticCraft types, so registering them is safe whether or not it is installed
        PneumaticAttachments.ATTACHMENT_TYPES.register(modEventBus);
        modEventBus.addListener(PneumaticNetwork::onRegisterPayloads);
        CompatBootstrap.registerBuiltInModules();
        // Added after the core listener on purpose: its setup work is queued first, so the API is bound first
        modEventBus.addListener(CompatBootstrap::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // enqueueWork so the API is bound on the main thread before any other mod's setup work runs
        event.enqueueWork(FluidCompatBootstrap::initialize);
    }
}
