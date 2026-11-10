package me.thanhtnt2.fluidcompat.config;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Settings of the compat modules, stored in {@code config/fluidcompat-compat.toml}.
 *
 * <p>It lives in its own file on purpose: the original {@link FluidCompatConfig} keys
 * (finite water, drain percent, ...) are never rewritten when a new mod gets supported.
 */
public final class FluidCompatModulesConfig {

    // Default class names (not block ids) so every block of a pipe family is covered, whatever its id
    private static final List<String> DEFAULT_PASSABLE_BLOCK_CLASSES = List.of(
            "aztech.modern_industrialization.pipes.impl.PipeBlock",
            "dev.technici4n.moderndynamics.pipe.PipeBlock",
            "mcjty.xnet.modules.cables.blocks.GenericCableBlock",
            "com.refinedmods.refinedstorage.common.networking.CableBlock",
            "com.refinedmods.refinedstorage.common.support.AbstractDirectionalCableBlock",
            "mekanism.common.block.transmitter.BlockTransmitter");

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue COMPAT_ENABLED;

    public static final ModConfigSpec.BooleanValue FF_PATCHES_DISABLED;
    public static final ModConfigSpec.BooleanValue FF_FAST_INTERACTION_CHECKS;

    public static final ModConfigSpec.BooleanValue LOG_FILE_ENABLED;
    public static final ModConfigSpec.BooleanValue LOG_VERBOSE;
    public static final ModConfigSpec.BooleanValue LOG_DEBUG_MODE;
    public static final ModConfigSpec.IntValue LOG_VERBOSE_THROTTLE_MS;

    public static final ModConfigSpec.BooleanValue ELECTRIC_PUMP_ENABLED;
    public static final ModConfigSpec.BooleanValue ELECTRIC_PUMP_WATER;
    public static final ModConfigSpec.BooleanValue ELECTRIC_PUMP_LAVA;
    public static final ModConfigSpec.BooleanValue ELECTRIC_PUMP_TAGGED_FLUIDS;

    public static final ModConfigSpec.BooleanValue SEAWATER_PUMP_ENABLED;
    public static final ModConfigSpec.IntValue SEAWATER_DRAIN_PERCENT;
    public static final ModConfigSpec.IntValue SEAWATER_MIN_TICKS_BETWEEN_INTAKES;
    public static final ModConfigSpec.EnumValue<ShortfallBehavior> SEAWATER_SHORTFALL;
    public static final ModConfigSpec.BooleanValue SEAWATER_DRAIN_THIN_WATER;

    public static final ModConfigSpec.BooleanValue MI_PUMP_DRAIN_THIN_WATER;

    public static final ModConfigSpec.BooleanValue IE_PUMP_FINITE;
    public static final ModConfigSpec.BooleanValue IE_PUMP_NO_COBBLE;
    public static final ModConfigSpec.BooleanValue IE_PUMP_DRAIN_THIN;

    public static final ModConfigSpec.BooleanValue MDG_PUMPS_ENABLED;

    public static final ModConfigSpec.BooleanValue ALEXS_DRAIN_ENABLED;
    public static final ModConfigSpec.IntValue ALEXS_DRAIN_MAX_LEVELS_PER_CYCLE;
    public static final ModConfigSpec.IntValue ALEXS_DRAIN_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue ALEXS_DRAIN_FILL_SPREAD;
    public static final ModConfigSpec.BooleanValue ALEXS_DRAIN_WORLD_EFFECTS;

    public static final ModConfigSpec.BooleanValue AUTO_BALANCED_ENABLED;
    public static final ModConfigSpec.BooleanValue AUTO_BALANCED_FOLLOW_FLOWING_FLUIDS;
    public static final ModConfigSpec.IntValue AUTO_BALANCED_TARGET_MSPT;
    public static final ModConfigSpec.IntValue AUTO_BALANCED_MAX_SLOWDOWN;

    public static final ModConfigSpec.BooleanValue CREATE_ENABLED;
    public static final ModConfigSpec.BooleanValue CREATE_EXCLUDE_BLOCKS;

    public static final ModConfigSpec.BooleanValue PNC_PICKUP_ENABLED;
    public static final ModConfigSpec.BooleanValue PNC_LEFTOVER_WATER;
    public static final ModConfigSpec.BooleanValue PNC_LEFTOVER_WITHOUT_UPGRADE;
    public static final ModConfigSpec.BooleanValue PNC_REQUIRE_OUTPUT_CONTAINER;
    public static final ModConfigSpec.BooleanValue PNC_LEFTOVER_NEEDS_OUTPUT;
    public static final ModConfigSpec.IntValue PNC_EMPTY_RETRY_TICKS;
    public static final ModConfigSpec.BooleanValue PNC_GUI_BUTTON;

    public static final ModConfigSpec.BooleanValue FLUIDLOGGED_ENABLED;
    public static final ModConfigSpec.BooleanValue FLUIDLOGGED_ACCEPT_DUPLICATION_RISK;
    public static final ModConfigSpec.BooleanValue FLUIDLOGGED_FLUIDLOGGABLE;
    public static final ModConfigSpec.BooleanValue FLUIDLOGGED_PERMEABLE;
    public static final ModConfigSpec.BooleanValue FLUIDLOGGED_SHAPE_INDEPENDENT;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FLUIDLOGGED_BLOCK_CLASSES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FLUIDLOGGED_EXTRA_BLOCKS;

    public static final ModConfigSpec.BooleanValue FF_PIPES_ENABLED;
    public static final ModConfigSpec.BooleanValue FF_PIPES_NON_DISPLACERS;
    public static final ModConfigSpec.EnumValue<WaterlogFlowOverride> FF_PIPES_WATERLOG_MODE;
    public static final ModConfigSpec.BooleanValue FF_PIPES_ALLOW_EXPERIMENTAL_MODES;
    public static final ModConfigSpec.BooleanValue FF_PIPES_CLEAN_ORPHAN_WATER;
    public static final ModConfigSpec.IntValue FF_PIPES_CLEANUP_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue FF_PIPES_CLEANUP_CHUNK_RADIUS;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("general");
        COMPAT_ENABLED = BUILDER
                .comment("Master switch for every compat module. Turn it off to run only the core Modern Industrialization hook.")
                .define("compatEnabled", true);
        BUILDER.pop();

        BUILDER.push("logging");
        LOG_FILE_ENABLED = BUILDER
                .comment("Write logs/fluidcompat.log in the game folder (the previous run is kept as fluidcompat-previous.log).")
                .define("fileEnabled", true);
        LOG_VERBOSE = BUILDER
                .comment("Also log every fluid intake. Useful for bug reports, noisy otherwise.")
                .define("verbose", false);
        LOG_DEBUG_MODE = BUILDER
                .comment("DEBUG MODE: log EVERYTHING into logs/fluidcompat.log: full config dump, every loaded mod,",
                        "every fluid intake (no throttling) and every block update of pipes. Files can get very large.",
                        "Pipe update tracing only starts after a game restart.")
                .define("debugMode", false);
        LOG_VERBOSE_THROTTLE_MS = BUILDER
                .comment("Minimum milliseconds between two verbose lines about the same machine.")
                .defineInRange("verboseThrottleMillis", 1000, 0, 600000);
        BUILDER.pop();

        BUILDER.push("auto_balanced");
        AUTO_BALANCED_ENABLED = BUILDER
                .comment("AUTO BALANCED COMPAT: when the server is slower than its target, Fluid Compat stretches the intervals of its own",
                        "optional periodic work (pipe cleanup, idle hopper checks) instead of adding to the lag. It never touches the",
                        "finite-water logic and never changes Flowing Fluids or Create settings.")
                .define("enabled", true);
        AUTO_BALANCED_FOLLOW_FLOWING_FLUIDS = BUILDER
                .comment("Use Flowing Fluids' own tick-time target (50 ms x its autoPerformanceMSPTargetMultiplier) while its auto performance",
                        "is not OFF, so both mods call the server 'slow' at the same moment.")
                .define("followFlowingFluidsTarget", true);
        AUTO_BALANCED_TARGET_MSPT = BUILDER
                .comment("Average milliseconds per tick above which Fluid Compat slows down its optional work (used when the option above does not apply).")
                .defineInRange("targetMillisPerTick", 45, 10, 100);
        AUTO_BALANCED_MAX_SLOWDOWN = BUILDER
                .comment("Largest factor the optional intervals are multiplied by while the server is slow. 1 = never slow down.")
                .defineInRange("maxSlowdown", 8, 1, 32);
        BUILDER.pop();

        BUILDER.push("create");
        CREATE_ENABLED = BUILDER
                .comment("Create is supported by Flowing Fluids itself (see create_* in flowing_fluids.json). Fluid Compat adds no Create hooks;",
                        "when this is true it only checks the combination at startup and writes the result to logs/fluidcompat.log.")
                .define("enabled", true);
        CREATE_EXCLUDE_BLOCKS = BUILDER
                .comment("Keep Create's blocks out of Fluid Compat's pipe features (non-displacers, pipe cleanup) so nothing overlaps with what",
                        "Flowing Fluids already does for Create.")
                .define("excludeCreateBlocks", true);
        BUILDER.pop();

        BUILDER.push("mekanism_electric_pump");
        ELECTRIC_PUMP_ENABLED = BUILDER
                .comment("Force the Mekanism Electric Pump to really remove source blocks instead of duplicating them.")
                .define("enabled", true);
        ELECTRIC_PUMP_WATER = BUILDER
                .comment("Apply to water (still requires fluids.finiteWater in fluidcompat-common.toml).")
                .define("water", true);
        ELECTRIC_PUMP_LAVA = BUILDER
                .comment("Apply to lava (still requires fluids.finiteLava in fluidcompat-common.toml).")
                .define("lava", true);
        ELECTRIC_PUMP_TAGGED_FLUIDS = BUILDER
                .comment("Apply to fluids in the tag fluidcompat:managed_fluids.")
                .define("taggedFluids", true);
        BUILDER.pop();

        BUILDER.push("mekanism_elements_seawater_pump");
        SEAWATER_PUMP_ENABLED = BUILDER
                .comment("Make the Mekanism Elements Seawater Pump drain the water source below it.")
                .define("enabled", true);
        SEAWATER_DRAIN_PERCENT = BUILDER
                .comment("World water consumed per unit of seawater made. 100 = one for one, 50 = twice as efficient.")
                .defineInRange("drainPercent", 100, 1, 1000);
        SEAWATER_MIN_TICKS_BETWEEN_INTAKES = BUILDER
                .comment("Minimum ticks between two drains of the same pump. 0 = no extra limit.")
                .defineInRange("minTicksBetweenIntakes", 0, 0, 72000);
        SEAWATER_SHORTFALL = BUILDER
                .comment("When the world cannot pay a whole cycle: SCALE_DOWN produces less, STOP produces nothing.")
                .defineEnum("shortfallBehavior", ShortfallBehavior.SCALE_DOWN);
        SEAWATER_DRAIN_THIN_WATER = BUILDER
                .comment("Let the Seawater Pump keep working when the water below it is a thin Flowing Fluids layer instead of a full",
                        "source block. Every cycle the world pays for the seawater out of that very cell, so without this the pump",
                        "stops after a cycle or two. Output is unchanged and always paid by the world.")
                .define("drainThinWater", true);
        BUILDER.pop();

        BUILDER.push("pneumaticcraft_pickup");
        PNC_PICKUP_ENABLED = BUILDER
                .comment("PneumaticCraft's Liquid Hopper and Gas Lift leave a water source block in place when it has two water",
                        "neighbours, which makes their water infinite. When this is true and water is finite, they remove it like any other source.",
                        "Drones (Liquid Import) already remove every source block, so they need no hook.")
                .define("enabled", true);
        PNC_LEFTOVER_WATER = BUILDER
                .comment("Liquid Hopper also drains the thin leftover water of a pond (partial levels that PneumaticCraft cannot pick up",
                        "because they are not source blocks). Needs Flowing Fluids. Each hopper has its own on/off button in its GUI.")
                .define("liquidHopperLeftoverWater", true);
        PNC_LEFTOVER_WITHOUT_UPGRADE = BUILDER
                .comment("Allow the leftover-water drain WITHOUT a Dispenser Upgrade. Off by default so it cannot be used as a shortcut:",
                        "with the upgrade the hopper takes whole source blocks too, without it only the thin leftover water.")
                .define("liquidHopperLeftoverWithoutUpgrade", false);
        PNC_REQUIRE_OUTPUT_CONTAINER = BUILDER
                .comment("[Ignored since 1.3.2, replaced by liquidHopperLeftoverNeedsOutput. Kept so existing files are not rewritten.]")
                .define("requireOutputContainer", true);
        PNC_LEFTOVER_NEEDS_OUTPUT = BUILDER
                .comment("Only drain leftover water while the hopper's output side has a container that can take water.",
                        "Off by default: the hopper's own tank fills up and then simply stops, like PneumaticCraft's own import.")
                .define("liquidHopperLeftoverNeedsOutput", false);
        PNC_EMPTY_RETRY_TICKS = BUILDER
                .comment("After a hopper found no water to drain it waits this many ticks before looking again. Higher = less work.")
                .defineInRange("emptyRetryTicks", 20, 1, 1200);
        PNC_GUI_BUTTON = BUILDER
                .comment("Show the Fluid Compat on/off button in the Liquid Hopper screen (client setting).")
                .define("guiButton", true);
        BUILDER.pop();

        BUILDER.push("fluidlogged_pipes");
        FLUIDLOGGED_ENABLED = BUILDER
                .comment("Register a Fluidlogged addon so fluids can enter and cross pipe blocks. Needs a restart.")
                .define("enabled", true);
        FLUIDLOGGED_ACCEPT_DUPLICATION_RISK = BUILDER
                .comment("DANGEROUS, keep false. Fluidlogged keeps its own hidden copy of the fluid inside a pipe, next to the",
                        "block's waterlogged flag. Together with Flowing Fluids this was seen to duplicate water.",
                        "The addon is therefore only registered when this is true.")
                .define("acceptDuplicationRisk", false);
        FLUIDLOGGED_FLUIDLOGGABLE = BUILDER
                .comment("Pipes can hold any fluid, so flowing fluid no longer destroys them.")
                .define("makeFluidloggable", true);
        FLUIDLOGGED_PERMEABLE = BUILDER
                .comment("Fluids flow through pipes instead of treating them as solid walls.")
                .define("makePermeable", true);
        FLUIDLOGGED_SHAPE_INDEPENDENT = BUILDER
                .comment("Let fluid pass whatever the pipe shape is. When false, Fluidlogged's shape-based permeability is used.")
                .define("shapeIndependent", true);
        FLUIDLOGGED_BLOCK_CLASSES = BUILDER
                .comment("Fully qualified Java class names. Every block that is an instance of one of them is covered.")
                .defineListAllowEmpty("blockClasses", DEFAULT_PASSABLE_BLOCK_CLASSES, () -> "",
                        FluidCompatModulesConfig::isNonBlankString);
        FLUIDLOGGED_EXTRA_BLOCKS = BUILDER
                .comment("Extra entries: block ids like \"modid:block\" or block tags like \"#modid:tag\".",
                        "Datapacks can also add blocks to the tag fluidcompat:fluid_passable_blocks.")
                .defineListAllowEmpty("extraBlocks", List.of(), () -> "",
                        FluidCompatModulesConfig::isNonBlankString);
        BUILDER.pop();

        BUILDER.push("flowing_fluids_pipes");
        FF_PIPES_ENABLED = BUILDER
                .comment("Teach Flowing Fluids that pipes are special blocks. Uses the same pipe list as fluidlogged_pipes.")
                .define("enabled", true);
        FF_PIPES_NON_DISPLACERS = BUILDER
                .comment("Register pipes through the Flowing Fluids API as blocks that never push fluid out of their cell.")
                .define("registerAsNonDisplacers", true);
        FF_PIPES_WATERLOG_MODE = BUILDER
                .comment("How Flowing Fluids treats waterlogged blocks (pipes). GLOBAL setting: it also affects slabs, stairs, etc.",
                        "Applied in memory when a world starts; nothing is written to Flowing Fluids' own config file.",
                        "  IGNORE (default, safe): pipes neither take nor give water through Flowing Fluids, so they cannot duplicate it.",
                        "  KEEP_FLOWING_FLUIDS_SETTING: change nothing (Flowing Fluids' own default).",
                        "  Any other value is EXPERIMENTAL: pipes then take part in the fluid simulation and water duplication was",
                        "  observed with it. It is ignored (IGNORE is used) unless allowExperimentalWaterlogModes is true.")
                .defineEnum("waterlogFlowMode", WaterlogFlowOverride.IGNORE);
        FF_PIPES_ALLOW_EXPERIMENTAL_MODES = BUILDER
                .comment("DANGEROUS: lets waterlogFlowMode use modes other than IGNORE / KEEP. Can duplicate water. Only for testing.")
                .define("allowExperimentalWaterlogModes", false);
        FF_PIPES_CLEAN_ORPHAN_WATER = BUILDER
                .comment("Remove the water left inside a waterlogged pipe once none of its non-pipe neighbours holds water.",
                        "Stops water cubes from hanging in the air around pipes and removes stuck water instead of copying it.")
                .define("cleanOrphanPipeWater", true);
        FF_PIPES_CLEANUP_INTERVAL_TICKS = BUILDER
                .comment("Ticks between two cleanup passes. Higher = less work, slower cleanup.")
                .defineInRange("cleanupIntervalTicks", 40, 5, 1200);
        FF_PIPES_CLEANUP_CHUNK_RADIUS = BUILDER
                .comment("Only pipes in loaded chunks within this many chunks of a player are cleaned. Lower = less work.")
                .defineInRange("cleanupChunkRadius", 4, 1, 16);
        BUILDER.pop();

        BUILDER.push("modern_industrialization_pumps");
        MI_PUMP_DRAIN_THIN_WATER = BUILDER
                .comment("Let Modern Industrialization water pumps also pump the thin, non-source water layers Flowing Fluids leaves behind,",
                        "so a pond can be drained completely. Output per pump tier stays exactly what Modern Industrialization defines",
                        "(bronze 125 mB, steel 250 mB, electric 2000 mB per water cell and cycle) and is always paid by the world.")
                .define("drainThinWater", true);
        BUILDER.pop();

        BUILDER.push("immersive_engineering");
        IE_PUMP_FINITE = BUILDER
                .comment("Stop the Immersive Engineering Fluid Pump from making endless water or lava out of pools with two source",
                        "blocks. With this on it takes real source blocks out of the world (1000 mB per block) like it does for oil.",
                        "Only applies to fluids Fluid Compat treats as finite, see fluidcompat-common.toml.")
                .define("finitePump", true);
        IE_PUMP_NO_COBBLE = BUILDER
                .comment("Immersive Engineering pumps replace every block they empty with cobblestone (its own pump_placeCobble option).",
                        "With this on, that replacement is skipped when the fluid is finite, so the emptied block stays empty and",
                        "Flowing Fluids can refill or drain the area naturally. Fluids that are not finite still get cobblestone.",
                        "IE warns that without cobblestone, pumping large amounts of fluid can cause lag.")
                .define("noCobbleForFiniteFluids", true);
        IE_PUMP_DRAIN_THIN = BUILDER
                .comment("Let the Immersive Engineering Fluid Pump also take the thin, non-source layers Flowing Fluids leaves behind,",
                        "so a pond can be emptied completely. Each layer is taken as exactly the water it holds (125 mB per level)",
                        "and paid by the world. Whole source blocks are untouched. Only for fluids Fluid Compat treats as finite.")
                .define("drainThinFluids", true);
        BUILDER.pop();

        BUILDER.push("mekanism_diesel_generators");
        MDG_PUMPS_ENABLED = BUILDER
                .comment("Make the Create-powered Shaft Pump and Inline Pump of Mekanism Diesel Generators take water out of the world",
                        "instead of making it. Each tick they pump, the world pays for exactly the water the pump receives (see",
                        "pumps.drainPercent in fluidcompat-common.toml). Needs Create; without it these pumps do not exist.")
                .define("enabled", true);
        BUILDER.pop();

        BUILDER.push("alexscaves_drain");
        ALEXS_DRAIN_ENABLED = BUILDER
                .comment("Make the Alex's Caves Drain (switched on by a Copper Valve or any redstone signal) work with finite fluids.",
                        "It then moves exactly the fluid it takes from above to the cavity below, level by level.")
                .define("enabled", true);
        ALEXS_DRAIN_MAX_LEVELS_PER_CYCLE = BUILDER
                .comment("Most fluid one Drain moves per cycle, in 1/8 bucket levels. 512 = 64 full blocks, the original Drain's limit.")
                .defineInRange("maxLevelsPerCycle", 512, 8, 8192);
        ALEXS_DRAIN_INTERVAL_TICKS = BUILDER
                .comment("Ticks between two cycles of an open Drain. 20 is the original speed. Higher = less work.")
                .defineInRange("intervalTicks", 20, 5, 200);
        ALEXS_DRAIN_FILL_SPREAD = BUILDER
                .comment("How many blocks sideways the released fluid may spread below the Drain. 10 is the original value.")
                .defineInRange("fillSpread", 10, 1, 16);
        ALEXS_DRAIN_WORLD_EFFECTS = BUILDER
                .comment("Play Alex's Caves' own drain sound and bubbles while the Drain works.")
                .define("worldEffects", true);
        BUILDER.pop();

        BUILDER.push("flowing_fluid_patches");
        FF_PATCHES_DISABLED = BUILDER
                .comment("Turn OFF Fluid Compat's small repairs for bugs inside Flowing Fluids (see the log file for the list).",
                        "These are mixins, so changing this needs a GAME RESTART. general.compatEnabled = false also turns them off.")
                .define("disablePatches", false);
        FF_FAST_INTERACTION_CHECKS = BUILDER
                .comment("Skip NeoForge's fluid interaction check (lava meets water and similar) when a liquid block only changed its level.",
                        "This is the biggest server tick saving of the patches with Flowing Fluids: the check ran on nearly every fluid update.",
                        "Reactions between two different fluids or blocks are still checked in full. Needs a GAME RESTART.",
                        "Turn it off if you ever see a fluid reaction that no longer happens.")
                .define("fastInteractionChecks", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private FluidCompatModulesConfig() {
    }

    private static boolean isNonBlankString(Object value) {
        return value instanceof String text && !text.isBlank();
    }
}
