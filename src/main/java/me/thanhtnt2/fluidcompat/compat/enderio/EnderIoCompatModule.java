package me.thanhtnt2.fluidcompat.compat.enderio;

import me.thanhtnt2.fluidcompat.api.compat.CompatModule;
import me.thanhtnt2.fluidcompat.compat.CompatMixinState;
import me.thanhtnt2.fluidcompat.compat.common.PipeBlockMatcher;
import me.thanhtnt2.fluidcompat.compat.enderio.mixin.EnderIoDrainMixinPlugin;
import me.thanhtnt2.fluidcompat.compat.enderio.mixin.EnderIoNiardMixinPlugin;
import me.thanhtnt2.fluidcompat.config.FluidCompatModulesConfig;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;

/**
 * Ender IO. What was checked in its bytecode (8.2.12-beta):
 * <ul>
 *   <li>The Drain removes only source blocks (it sets the block to air and receives one bucket), so it never made
 *       fluid. It ignores the thin layers Flowing Fluids leaves behind and stops when the block under it is one.</li>
 *   <li>The Niard places a source block and pays a bucket, but over a thin layer of its own fluid it pays more than
 *       the world gains.</li>
 *   <li>The Industrial Insulation block is a sponge (it removes water, never creates it) and no other machine takes
 *       fluid from or puts fluid into the world. The conduit bundle is a waterloggable block like any other pipe.</li>
 * </ul>
 * The hooks are mixins, see {@code mixin.impl}; the rules live in {@link EnderIoThinLayer} and {@link EnderIoNiardRefund}.
 */
public final class EnderIoCompatModule implements CompatModule {

    public static final String ID = "enderio";
    private static final String CONDUIT_BUNDLE_CLASS = "com.enderio.enderio.content.conduits.bundle.ConduitBundleBlock";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String modId() {
        return "enderio";
    }

    @Override
    public String displayName() {
        return "Ender IO (Drain, Niard, conduit bundle)";
    }

    @Override
    public void initialize() {
        if (FluidCompatModulesConfig.ENDERIO_CONDUITS_AS_PIPES.get()) {
            // Added next to the user's pipe list, which is never touched; the pipes module reads it after this runs
            PipeBlockMatcher.addModuleClassName(CONDUIT_BUNDLE_CLASS);
            FluidCompatLog.info("[enderio] conduit bundle added to the submerged pipe blocks");
        }
        if (!FluidCompatModulesConfig.ENDERIO_ENABLED.get()) {
            FluidCompatLog.info("[enderio] Drain and Niard hooks disabled in config");
            return;
        }
        // Loading a class (without running anything in it) makes Mixin ask the guards now, so the verdicts are known
        // at report time; "allowed" only says the guard accepted the code, the hooks log "for the first time" when used
        loadClass(EnderIoDrainMixinPlugin.TARGET);
        loadClass(EnderIoNiardMixinPlugin.TARGET);
        report("Drain", EnderIoDrainMixinPlugin.MODULE_ID, "the Drain keeps ignoring thin water layers");
        report("Niard", EnderIoNiardMixinPlugin.MODULE_ID, "the Niard keeps losing the water of a thin layer it fills");
    }

    private static void loadClass(String name) {
        try {
            Class.forName(name, false, EnderIoCompatModule.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError error) {
            FluidCompatLog.warn("[enderio] could not load " + name + ": " + error);
        }
    }

    private static void report(String machine, String verdictId, String consequence) {
        if (CompatMixinState.isApplied(verdictId)) {
            FluidCompatLog.info("[enderio] " + machine + " hook allowed");
        } else if (CompatMixinState.hasVerdict(verdictId)) {
            FluidCompatLog.warn("[enderio] " + machine + " hook NOT allowed (see 'guard refused' in latest.log); " + consequence);
        } else {
            FluidCompatLog.warn("[enderio] " + machine + " hook was never asked (its mixin config did not load)");
        }
    }
}
