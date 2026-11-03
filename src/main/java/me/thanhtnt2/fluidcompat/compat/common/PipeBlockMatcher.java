package me.thanhtnt2.fluidcompat.compat.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import me.thanhtnt2.fluidcompat.api.FluidCompatModulesTags;
import me.thanhtnt2.fluidcompat.config.FluidCompatModulesConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Resolves the pipe-like blocks from the shared config list (class names, ids and tags).
 * Matching by class name covers a whole pipe family without hard-coded ids or links to the pipe mods.
 */
public final class PipeBlockMatcher {

    private PipeBlockMatcher() {
    }

    /** Must run after all registries are filled, which is true from common setup onwards. */
    public static List<Block> collectBlocks() {
        Set<String> classNames = new HashSet<>(FluidCompatModulesConfig.FLUIDLOGGED_BLOCK_CLASSES.get());
        Set<Block> matched = new LinkedHashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (isInstanceOfAnyNamedClass(block.getClass(), classNames) && !ExternalHandledBlocks.isHandledElsewhere(block)) {
                matched.add(block);
            }
        }
        for (String entry : FluidCompatModulesConfig.FLUIDLOGGED_EXTRA_BLOCKS.get()) {
            addBlockEntry(entry, matched);
        }
        return List.copyOf(matched);
    }

    public static List<TagKey<Block>> collectTags() {
        List<TagKey<Block>> tags = new ArrayList<>();
        tags.add(FluidCompatModulesTags.FLUID_PASSABLE_BLOCKS);
        for (String entry : FluidCompatModulesConfig.FLUIDLOGGED_EXTRA_BLOCKS.get()) {
            if (!entry.startsWith("#")) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(entry.substring(1).trim());
            if (id != null) {
                tags.add(TagKey.create(Registries.BLOCK, id));
            }
        }
        return tags;
    }

    private static boolean isInstanceOfAnyNamedClass(Class<?> type, Set<String> classNames) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (classNames.contains(current.getName())) {
                return true;
            }
        }
        return false;
    }

    private static void addBlockEntry(String entry, Set<Block> target) {
        if (entry.startsWith("#")) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(entry.trim());
        if (id != null) {
            BuiltInRegistries.BLOCK.getOptional(id).filter(block -> !ExternalHandledBlocks.isHandledElsewhere(block)).ifPresent(target::add);
        }
    }
}
