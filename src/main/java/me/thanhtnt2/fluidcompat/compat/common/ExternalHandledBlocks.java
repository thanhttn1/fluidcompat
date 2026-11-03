package me.thanhtnt2.fluidcompat.compat.common;

import me.thanhtnt2.fluidcompat.config.FluidCompatModulesConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;

/**
 * Blocks that another mod already takes care of together with Flowing Fluids. Fluid Compat keeps its own pipe
 * features away from them so the two never overlap. Today that is Create (Flowing Fluids has its own Create support).
 */
public final class ExternalHandledBlocks {

    private static final String CREATE_MOD_ID = "create";
    private static final String CREATE_PACKAGE_PREFIX = "com.simibubi.create.";

    private ExternalHandledBlocks() {
    }

    public static boolean isHandledElsewhere(Block block) {
        if (!createExclusionActive()) {
            return false;
        }
        return CREATE_MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())
                || block.getClass().getName().startsWith(CREATE_PACKAGE_PREFIX);
    }

    public static boolean createExclusionActive() {
        return FluidCompatModulesConfig.CREATE_ENABLED.get()
                && FluidCompatModulesConfig.CREATE_EXCLUDE_BLOCKS.get()
                && ModList.get().isLoaded(CREATE_MOD_ID);
    }
}
