package me.thanhtnt2.fluidcompat.compat.modernindustrialization;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Recognises Modern Industrialization water pumps without linking against its classes,
 * so this helper is safe to call (and to load) when MI is not installed.
 */
public final class MiPumpTargeting {

    private static final String ABSTRACT_WATER_PUMP_CLASS =
            "aztech.modern_industrialization.machines.blockentities.AbstractWaterPumpBlockEntity";

    private MiPumpTargeting() {
    }

    public static boolean isWaterPump(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }
        // Walk the hierarchy by name: bronze, steel and electric pumps all extend the same abstract class
        for (Class<?> type = blockEntity.getClass(); type != null; type = type.getSuperclass()) {
            if (ABSTRACT_WATER_PUMP_CLASS.equals(type.getName())) {
                return true;
            }
        }
        return false;
    }
}
