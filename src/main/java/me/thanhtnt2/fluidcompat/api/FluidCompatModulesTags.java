package me.thanhtnt2.fluidcompat.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Tags added together with the compat modules. Kept apart from {@link FluidCompatTags} to leave that class untouched. */
public final class FluidCompatModulesTags {

    /** Small blocks (pipes, cables) that fluids should flow through instead of treating them as walls. */
    public static final TagKey<Block> FLUID_PASSABLE_BLOCKS = blockTag("fluid_passable_blocks");

    /** Pump items of the compat modules that get the explanation page in JEI. */
    public static final TagKey<Item> COMPAT_FINITE_PUMP_ITEMS = itemTag("compat_finite_pumps");

    private FluidCompatModulesTags() {
    }

    private static TagKey<Block> blockTag(String path) {
        return TagKey.create(Registries.BLOCK, id(path));
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, id(path));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FluidCompatApi.MOD_ID, path);
    }
}
