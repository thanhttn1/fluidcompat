package me.thanhtnt2.fluidcompat.logging;

import me.thanhtnt2.fluidcompat.compat.CompatMixinState;
import me.thanhtnt2.fluidcompat.config.FluidCompatModulesConfig;
import me.thanhtnt2.fluidcompat.config.FluidCompatConfig;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

/** Debug mode only: writes everything a bug report could need into the log file at startup. */
public final class DebugSnapshot {

    private DebugSnapshot() {
    }

    public static void logAll() {
        FluidCompatLog.info("=== DEBUG SNAPSHOT START ===");
        logEnvironment();
        logMods();
        logMixinState();
        logCoreConfig();
        logCompatConfig();
        FluidCompatLog.info("=== DEBUG SNAPSHOT END ===");
    }

    private static void logEnvironment() {
        FluidCompatLog.info("game dir: {}", FMLPaths.GAMEDIR.get());
        FluidCompatLog.info("config dir: {}", FMLPaths.CONFIGDIR.get());
        FluidCompatLog.info("java: {} {} ({})", System.getProperty("java.vendor"), System.getProperty("java.version"), System.getProperty("os.name"));
        FluidCompatLog.info("max memory: {} MB", Runtime.getRuntime().maxMemory() / (1024 * 1024));
    }

    private static void logMods() {
        StringBuilder mods = new StringBuilder();
        ModList.get().getMods().forEach(mod -> mods.append(mod.getModId()).append('=').append(mod.getVersion()).append(' '));
        FluidCompatLog.info("all loaded mods ({}): {}", ModList.get().getMods().size(), mods.toString().trim());
    }

    private static void logMixinState() {
        for (String moduleId : new String[] {"modern_industrialization", "mekanism", "mekanism_elements", "mekanism_diesel_generators", "alexscaves", "immersiveengineering", "pneumaticcraft", "pneumaticcraft_hopper"}) {
            FluidCompatLog.info("mixin applied [{}] = {}", moduleId, CompatMixinState.isApplied(moduleId));
        }
    }

    private static void logCoreConfig() {
        FluidCompatLog.info("config[general] enableFiniteFluids={} backendPreference={}",
                FluidCompatConfig.ENABLE_FINITE_FLUIDS.get(), FluidCompatConfig.BACKEND_PREFERENCE.get());
        FluidCompatLog.info("config[fluids] finiteWater={} finiteLava={} finiteTaggedFluids={}",
                FluidCompatConfig.FINITE_WATER.get(), FluidCompatConfig.FINITE_LAVA.get(), FluidCompatConfig.FINITE_TAGGED_FLUIDS.get());
        FluidCompatLog.info("config[pumps] drainPercent={} minTicksBetweenIntakes={}",
                FluidCompatConfig.DRAIN_PERCENT.get(), FluidCompatConfig.MIN_TICKS_BETWEEN_INTAKES.get());
    }

    private static void logCompatConfig() {
        FluidCompatLog.info("config[compat] compatEnabled={} logFile={} verbose={} debugMode={} throttleMs={}",
                FluidCompatModulesConfig.COMPAT_ENABLED.get(), FluidCompatModulesConfig.LOG_FILE_ENABLED.get(),
                FluidCompatModulesConfig.LOG_VERBOSE.get(), FluidCompatModulesConfig.LOG_DEBUG_MODE.get(),
                FluidCompatModulesConfig.LOG_VERBOSE_THROTTLE_MS.get());
        FluidCompatLog.info("config[mekanism_electric_pump] enabled={} water={} lava={} tagged={}",
                FluidCompatModulesConfig.ELECTRIC_PUMP_ENABLED.get(), FluidCompatModulesConfig.ELECTRIC_PUMP_WATER.get(),
                FluidCompatModulesConfig.ELECTRIC_PUMP_LAVA.get(), FluidCompatModulesConfig.ELECTRIC_PUMP_TAGGED_FLUIDS.get());
        FluidCompatLog.info("config[mekanism_elements_seawater_pump] enabled={} drainPercent={} minTicks={} shortfall={} drainThinWater={}",
                FluidCompatModulesConfig.SEAWATER_PUMP_ENABLED.get(), FluidCompatModulesConfig.SEAWATER_DRAIN_PERCENT.get(),
                FluidCompatModulesConfig.SEAWATER_MIN_TICKS_BETWEEN_INTAKES.get(), FluidCompatModulesConfig.SEAWATER_SHORTFALL.get(),
                FluidCompatModulesConfig.SEAWATER_DRAIN_THIN_WATER.get());
        FluidCompatLog.info("config[mekanism_diesel_generators] enabled={}", FluidCompatModulesConfig.MDG_PUMPS_ENABLED.get());
        FluidCompatLog.info("config[auto_balanced] enabled={} followFlowingFluids={} targetMs={} maxSlowdown={}",
                FluidCompatModulesConfig.AUTO_BALANCED_ENABLED.get(), FluidCompatModulesConfig.AUTO_BALANCED_FOLLOW_FLOWING_FLUIDS.get(),
                FluidCompatModulesConfig.AUTO_BALANCED_TARGET_MSPT.get(), FluidCompatModulesConfig.AUTO_BALANCED_MAX_SLOWDOWN.get());
        FluidCompatLog.info("config[create] enabled={} excludeCreateBlocks={}",
                FluidCompatModulesConfig.CREATE_ENABLED.get(), FluidCompatModulesConfig.CREATE_EXCLUDE_BLOCKS.get());
        FluidCompatLog.info("config[pneumaticcraft_pickup] enabled={} leftoverWater={} withoutUpgrade={} leftoverNeedsOutput={} retryTicks={} guiButton={}",
                FluidCompatModulesConfig.PNC_PICKUP_ENABLED.get(), FluidCompatModulesConfig.PNC_LEFTOVER_WATER.get(),
                FluidCompatModulesConfig.PNC_LEFTOVER_WITHOUT_UPGRADE.get(), FluidCompatModulesConfig.PNC_LEFTOVER_NEEDS_OUTPUT.get(),
                FluidCompatModulesConfig.PNC_EMPTY_RETRY_TICKS.get(), FluidCompatModulesConfig.PNC_GUI_BUTTON.get());
        FluidCompatLog.info("config[fluidlogged_pipes] acceptDuplicationRisk={}", FluidCompatModulesConfig.FLUIDLOGGED_ACCEPT_DUPLICATION_RISK.get());
        FluidCompatLog.info("config[fluidlogged_pipes] enabled={} fluidloggable={} permeable={} shapeIndependent={} classes={} extra={}",
                FluidCompatModulesConfig.FLUIDLOGGED_ENABLED.get(), FluidCompatModulesConfig.FLUIDLOGGED_FLUIDLOGGABLE.get(),
                FluidCompatModulesConfig.FLUIDLOGGED_PERMEABLE.get(), FluidCompatModulesConfig.FLUIDLOGGED_SHAPE_INDEPENDENT.get(),
                FluidCompatModulesConfig.FLUIDLOGGED_BLOCK_CLASSES.get(), FluidCompatModulesConfig.FLUIDLOGGED_EXTRA_BLOCKS.get());
        FluidCompatLog.info("config[flowing_fluids_pipes] enabled={} nonDisplacers={} waterlogFlowMode={} allowExperimental={}",
                FluidCompatModulesConfig.FF_PIPES_ENABLED.get(), FluidCompatModulesConfig.FF_PIPES_NON_DISPLACERS.get(),
                FluidCompatModulesConfig.FF_PIPES_WATERLOG_MODE.get(), FluidCompatModulesConfig.FF_PIPES_ALLOW_EXPERIMENTAL_MODES.get());
        FluidCompatLog.info("config[flowing_fluid_patches] disablePatches={} fastInteractionChecks={}",
                FluidCompatModulesConfig.FF_PATCHES_DISABLED.get(), FluidCompatModulesConfig.FF_FAST_INTERACTION_CHECKS.get());
        FluidCompatLog.info("config[flowing_fluids_pipes] cleanOrphanPipeWater={} intervalTicks={} chunkRadius={}",
                FluidCompatModulesConfig.FF_PIPES_CLEAN_ORPHAN_WATER.get(), FluidCompatModulesConfig.FF_PIPES_CLEANUP_INTERVAL_TICKS.get(),
                FluidCompatModulesConfig.FF_PIPES_CLEANUP_CHUNK_RADIUS.get());
    }
}
