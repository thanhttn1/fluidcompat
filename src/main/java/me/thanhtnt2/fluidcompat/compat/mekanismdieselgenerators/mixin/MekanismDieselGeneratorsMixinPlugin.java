package me.thanhtnt2.fluidcompat.compat.mekanismdieselgenerators.mixin;

import java.util.List;

import me.thanhtnt2.fluidcompat.compat.GuardedCompatMixinPlugin;
import me.thanhtnt2.fluidcompat.compat.mekanismdieselgenerators.guard.DrawContract;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.service.MixinService;

/**
 * Applies the pump hook only when the Shaft Pump and Inline Pump class still works the way {@link DrawContract} expects.
 * The class is only loaded when Create is present, so without Create this plugin is never asked at all.
 */
public final class MekanismDieselGeneratorsMixinPlugin extends GuardedCompatMixinPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("FluidCompat/Mixin");

    public static final String MODULE_ID = "mekanism_diesel_generators";
    private static final String PUMP_CLASS = "com.jambonbeurre78.mdg.compat.create.AuxDriveBlockEntity";
    // The pump extends this Create class; without Create the pumps do not exist and there is nothing to hook
    private static final String CREATE_BASE_CLASS = "com.simibubi.create.content.kinetics.base.KineticBlockEntity";

    public MekanismDieselGeneratorsMixinPlugin() {
        super(MODULE_ID, PUMP_CLASS, MekanismDieselGeneratorsMixinPlugin::pumpStillMatches);
    }

    private static boolean pumpStillMatches(ClassNode pump) {
        if (!createIsPresent()) {
            LOGGER.info("[{}] Create not found, so the Shaft Pump and Inline Pump do not exist; hook not needed", MODULE_ID);
            return false;
        }
        List<String> problems = DrawContract.problemsOf(pump);
        if (problems.isEmpty()) {
            return true;
        }
        LOGGER.warn("[{}] the Shaft Pump / Inline Pump no longer works the way Fluid Compat expects, so its hook is OFF "
                + "and the pumps keep making endless water. Update Fluid Compat. Reason: {}", MODULE_ID, String.join("; ", problems));
        return false;
    }

    private static boolean createIsPresent() {
        try {
            MixinService.getService().getBytecodeProvider().getClassNode(CREATE_BASE_CLASS);
            return true;
        } catch (Exception exception) {
            // Catch-all on purpose: the checked exception types differ between Mixin versions,
            // and "class not found" simply means Create is not installed
            return false;
        }
    }
}
