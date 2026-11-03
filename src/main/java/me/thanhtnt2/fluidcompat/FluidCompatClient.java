package me.thanhtnt2.fluidcompat;

import me.thanhtnt2.fluidcompat.client.pneumaticcraft.PneumaticHopperScreenHook;
import me.thanhtnt2.fluidcompat.client.yacl.YaclConfigScreenFactory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only entry point; never loaded on dedicated servers. */
@Mod(value = FluidCompat.MODID, dist = Dist.CLIENT)
public final class FluidCompatClient {

    private static final String YACL_MOD_ID = "yet_another_config_lib_v3";

    public FluidCompatClient(ModContainer container) {
        // Players get the YACL screen (with the "Mod Compat List" tab) when YACL is installed,
        // otherwise NeoForge's built-in configuration screen
        IConfigScreenFactory screenFactory = ModList.get().isLoaded(YACL_MOD_ID)
                ? (modContainer, parentScreen) -> YaclConfigScreenFactory.create(parentScreen)
                : ConfigurationScreen::new;
        container.registerExtensionPoint(IConfigScreenFactory.class, screenFactory);
        if (ModList.get().isLoaded("pneumaticcraft")) {
            PneumaticHopperScreenHook.register();
        }
    }
}
