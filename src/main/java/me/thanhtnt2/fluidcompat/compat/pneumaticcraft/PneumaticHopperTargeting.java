package me.thanhtnt2.fluidcompat.compat.pneumaticcraft;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Recognises a PneumaticCraft Liquid Hopper by its block id, so no PneumaticCraft class is needed. */
public final class PneumaticHopperTargeting {

    private static final ResourceLocation LIQUID_HOPPER_BLOCK_ID =
            ResourceLocation.fromNamespaceAndPath("pneumaticcraft", "liquid_hopper");

    private PneumaticHopperTargeting() {
    }

    public static boolean isLiquidHopper(BlockEntity blockEntity) {
        return blockEntity != null
                && LIQUID_HOPPER_BLOCK_ID.equals(BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock()));
    }
}
