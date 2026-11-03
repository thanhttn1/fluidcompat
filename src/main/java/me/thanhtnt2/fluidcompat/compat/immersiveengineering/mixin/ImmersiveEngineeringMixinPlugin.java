package me.thanhtnt2.fluidcompat.compat.immersiveengineering.mixin;

import java.util.List;
import java.util.Set;

import me.thanhtnt2.fluidcompat.compat.CompatMixinState;
import me.thanhtnt2.fluidcompat.compat.immersiveengineering.guard.PumpContract;
import me.thanhtnt2.fluidcompat.compat.immersiveengineering.guard.PumpGuardStatus;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/**
 * Applies the Immersive Engineering mixins all together or not at all.
 *
 * <p>The hooks change how the Fluid Pump treats finite fluids in several places that depend on each other, so a
 * pump that is only partly changed would behave worse than either extreme. {@link PumpContract} reads the pump's
 * bytecode once; if anything it relies on is no longer there, every Immersive Engineering hook stays off, the
 * reason is logged, and the pump simply works as Immersive Engineering made it.
 */
public final class ImmersiveEngineeringMixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("FluidCompat/Mixin");
    private static final String MODULE_ID = "immersiveengineering";
    private static final String PUMP_CLASS = "blusunrize.immersiveengineering.common.blocks.metal.FluidPumpBlockEntity";
    private static final String UTILS_CLASS = "blusunrize.immersiveengineering.common.util.Utils";

    // One check for the whole run: every mixin of this config asks, only the first one reads any bytecode
    private static volatile PumpContract.Verdict verdict;

    @Override
    public boolean shouldApplyMixin(String targetClass, String mixinClassName) {
        return currentVerdict().ok();
    }

    private static synchronized PumpContract.Verdict currentVerdict() {
        if (verdict == null) {
            verdict = checkImmersiveEngineering();
            CompatMixinState.record(MODULE_ID, verdict.ok());
        }
        return verdict;
    }

    private static PumpContract.Verdict checkImmersiveEngineering() {
        ClassNode pump;
        ClassNode utils;
        try {
            pump = MixinService.getService().getBytecodeProvider().getClassNode(PUMP_CLASS);
            utils = MixinService.getService().getBytecodeProvider().getClassNode(UTILS_CLASS);
        } catch (Exception exception) {
            // Catch-all on purpose: the checked exception types differ between Mixin versions,
            // and "class not found" simply means Immersive Engineering is not installed
            LOGGER.info("[{}] Immersive Engineering not found, hooks disabled", MODULE_ID);
            return PumpContract.Verdict.notInstalled();
        }
        PumpContract.Verdict result = PumpContract.verify(pump, utils);
        if (!result.ok()) {
            PumpGuardStatus.disable(result.describe());
            LOGGER.warn("[{}] the Fluid Pump no longer works the way Fluid Compat expects, so ALL Immersive Engineering "
                    + "hooks are switched off and the pump behaves like in plain Immersive Engineering. Update Fluid Compat. "
                    + "Reason: {}", MODULE_ID, result.describe());
        }
        return result;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClass, ClassNode targetClassNode, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClass, ClassNode targetClassNode, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
