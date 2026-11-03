package me.thanhtnt2.fluidcompat.integration.jei;

import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import me.thanhtnt2.fluidcompat.api.FluidCompatTags;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Adds an explanation page to every item in the {@code fluidcompact:finite_pumps} tag.
 * Discovered by JEI's annotation scan, so it is never loaded when JEI is absent.
 */
@JeiPlugin
public final class FluidCompatJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(FluidCompatApi.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Tag driven so addon pumps can get the same page with a plain JSON tag
        BuiltInRegistries.ITEM.getTag(FluidCompatTags.FINITE_PUMP_ITEMS).ifPresent(pumps ->
                pumps.forEach(holder -> registration.addIngredientInfo(
                        holder.value(), Component.translatable("jei.fluidcompact.finite_pump"))));
    }
}
