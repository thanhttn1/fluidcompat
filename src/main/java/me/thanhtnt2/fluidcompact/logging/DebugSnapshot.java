package me.thanhtnt2.fluidcompact.logging;

import me.thanhtnt2.fluidcompact.compat.CompatMixinState;
import me.thanhtnt2.fluidcompact.config.FluidCompactCompatConfig;
import me.thanhtnt2.fluidcompact.config.FluidCompactConfig;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

/** Debug mode only: writes everything a bug report could need into the log file at startup. */
public final class DebugSnapshot {

    private DebugSnapshot() {
    }

    public static void logAll() {
        FluidCompactLog.info("=== DEBUG SNAPSHOT START ===");
        logEnvironment();
        logMods();
        logMixinState();
        logCoreConfig();
        logCompatConfig();
        FluidCompactLog.info("=== DEBUG SNAPSHOT END ===");
    }

    private static void logEnvironment() {
        FluidCompactLog.info("game dir: {}", FMLPaths.GAMEDIR.get());
        FluidCompactLog.info("config dir: {}", FMLPaths.CONFIGDIR.get());
        FluidCompactLog.info("java: {} {} ({})", System.getProperty("java.vendor"), System.getProperty("java.version"), System.getProperty("os.name"));
        FluidCompactLog.info("max memory: {} MB", Runtime.getRuntime().maxMemory() / (1024 * 1024));
    }

    private static void logMods() {
        StringBuilder mods = new StringBuilder();
        ModList.get().getMods().forEach(mod -> mods.append(mod.getModId()).append('=').append(mod.getVersion()).append(' '));
        FluidCompactLog.info("all loaded mods ({}): {}", ModList.get().getMods().size(), mods.toString().trim());
    }

    private static void logMixinState() {
        for (String moduleId : new String[] {"modern_industrialization", "mekanism", "mekanism_elements", "mekanism_diesel_generators", "alexscaves", "immersiveengineering", "pneumaticcraft", "pneumaticcraft_hopper"}) {
            FluidCompactLog.info("mixin applied [{}] = {}", moduleId, CompatMixinState.isApplied(moduleId));
        }
    }

    private static void logCoreConfig() {
        FluidCompactLog.info("config[general] enableFiniteFluids={} backendPreference={}",
                FluidCompactConfig.ENABLE_FINITE_FLUIDS.get(), FluidCompactConfig.BACKEND_PREFERENCE.get());
        FluidCompactLog.info("config[fluids] finiteWater={} finiteLava={} finiteTaggedFluids={}",
                FluidCompactConfig.FINITE_WATER.get(), FluidCompactConfig.FINITE_LAVA.get(), FluidCompactConfig.FINITE_TAGGED_FLUIDS.get());
        FluidCompactLog.info("config[pumps] drainPercent={} minTicksBetweenIntakes={}",
                FluidCompactConfig.DRAIN_PERCENT.get(), FluidCompactConfig.MIN_TICKS_BETWEEN_INTAKES.get());
    }

    private static void logCompatConfig() {
        FluidCompactLog.info("config[compat] compatEnabled={} logFile={} verbose={} debugMode={} throttleMs={}",
                FluidCompactCompatConfig.COMPAT_ENABLED.get(), FluidCompactCompatConfig.LOG_FILE_ENABLED.get(),
                FluidCompactCompatConfig.LOG_VERBOSE.get(), FluidCompactCompatConfig.LOG_DEBUG_MODE.get(),
                FluidCompactCompatConfig.LOG_VERBOSE_THROTTLE_MS.get());
        FluidCompactLog.info("config[mekanism_electric_pump] enabled={} water={} lava={} tagged={}",
                FluidCompactCompatConfig.ELECTRIC_PUMP_ENABLED.get(), FluidCompactCompatConfig.ELECTRIC_PUMP_WATER.get(),
                FluidCompactCompatConfig.ELECTRIC_PUMP_LAVA.get(), FluidCompactCompatConfig.ELECTRIC_PUMP_TAGGED_FLUIDS.get());
        FluidCompactLog.info("config[mekanism_elements_seawater_pump] enabled={} drainPercent={} minTicks={} shortfall={}",
                FluidCompactCompatConfig.SEAWATER_PUMP_ENABLED.get(), FluidCompactCompatConfig.SEAWATER_DRAIN_PERCENT.get(),
                FluidCompactCompatConfig.SEAWATER_MIN_TICKS_BETWEEN_INTAKES.get(), FluidCompactCompatConfig.SEAWATER_SHORTFALL.get());
        FluidCompactLog.info("config[mekanism_diesel_generators] enabled={}", FluidCompactCompatConfig.MDG_PUMPS_ENABLED.get());
        FluidCompactLog.info("config[auto_balanced] enabled={} followFlowingFluids={} targetMs={} maxSlowdown={}",
                FluidCompactCompatConfig.AUTO_BALANCED_ENABLED.get(), FluidCompactCompatConfig.AUTO_BALANCED_FOLLOW_FLOWING_FLUIDS.get(),
                FluidCompactCompatConfig.AUTO_BALANCED_TARGET_MSPT.get(), FluidCompactCompatConfig.AUTO_BALANCED_MAX_SLOWDOWN.get());
        FluidCompactLog.info("config[create] enabled={} excludeCreateBlocks={}",
                FluidCompactCompatConfig.CREATE_ENABLED.get(), FluidCompactCompatConfig.CREATE_EXCLUDE_BLOCKS.get());
        FluidCompactLog.info("config[pneumaticcraft_pickup] enabled={} leftoverWater={} withoutUpgrade={} leftoverNeedsOutput={} retryTicks={} guiButton={}",
                FluidCompactCompatConfig.PNC_PICKUP_ENABLED.get(), FluidCompactCompatConfig.PNC_LEFTOVER_WATER.get(),
                FluidCompactCompatConfig.PNC_LEFTOVER_WITHOUT_UPGRADE.get(), FluidCompactCompatConfig.PNC_LEFTOVER_NEEDS_OUTPUT.get(),
                FluidCompactCompatConfig.PNC_EMPTY_RETRY_TICKS.get(), FluidCompactCompatConfig.PNC_GUI_BUTTON.get());
        FluidCompactLog.info("config[fluidlogged_pipes] acceptDuplicationRisk={}", FluidCompactCompatConfig.FLUIDLOGGED_ACCEPT_DUPLICATION_RISK.get());
        FluidCompactLog.info("config[fluidlogged_pipes] enabled={} fluidloggable={} permeable={} shapeIndependent={} classes={} extra={}",
                FluidCompactCompatConfig.FLUIDLOGGED_ENABLED.get(), FluidCompactCompatConfig.FLUIDLOGGED_FLUIDLOGGABLE.get(),
                FluidCompactCompatConfig.FLUIDLOGGED_PERMEABLE.get(), FluidCompactCompatConfig.FLUIDLOGGED_SHAPE_INDEPENDENT.get(),
                FluidCompactCompatConfig.FLUIDLOGGED_BLOCK_CLASSES.get(), FluidCompactCompatConfig.FLUIDLOGGED_EXTRA_BLOCKS.get());
        FluidCompactLog.info("config[flowing_fluids_pipes] enabled={} nonDisplacers={} waterlogFlowMode={} allowExperimental={}",
                FluidCompactCompatConfig.FF_PIPES_ENABLED.get(), FluidCompactCompatConfig.FF_PIPES_NON_DISPLACERS.get(),
                FluidCompactCompatConfig.FF_PIPES_WATERLOG_MODE.get(), FluidCompactCompatConfig.FF_PIPES_ALLOW_EXPERIMENTAL_MODES.get());
        FluidCompactLog.info("config[flowing_fluids_pipes] cleanOrphanPipeWater={} intervalTicks={} chunkRadius={}",
                FluidCompactCompatConfig.FF_PIPES_CLEAN_ORPHAN_WATER.get(), FluidCompactCompatConfig.FF_PIPES_CLEANUP_INTERVAL_TICKS.get(),
                FluidCompactCompatConfig.FF_PIPES_CLEANUP_CHUNK_RADIUS.get());
    }
}
