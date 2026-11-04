package me.thanhtnt2.fluidcompat.compat.flowingfluids.patches;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.neoforged.fml.loading.FMLPaths;

/**
 * Decides, before any config is loaded, whether the Flowing Fluids patches may be applied.
 *
 * <p>Mixins are applied while the game is still starting, long before NeoForge reads {@code ModConfigSpec} files,
 * so the usual config values cannot be asked yet. This class reads the two relevant keys straight from the TOML
 * file instead. That is also why changing them needs a game restart. It deliberately touches no Minecraft class:
 * it runs inside the mixin plugin, where those classes are not ready.
 *
 * <p>A missing or unreadable file means "defaults", which means the patches are on.
 */
public final class PatchSwitch {

    private static final String CONFIG_FILE_NAME = "fluidcompat-compat.toml";
    // Mixins are chosen before the settings are copied from the old file name, so a first start after an update looks here too
    private static final String LEGACY_CONFIG_FILE_NAME = "fluidcompact-compat.toml";
    private static final String GENERAL_SECTION = "general";
    private static final String PATCHES_SECTION = "flowing_fluid_patches";
    private static final Pattern SECTION_HEADER = Pattern.compile("^\\s*\\[([^\\]]+)]\\s*(#.*)?$");
    private static final Pattern BOOLEAN_ENTRY = Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s*=\\s*(true|false)\\b.*$");

    private PatchSwitch() {
    }

    /**
     * @return why the patches must stay off, or {@code null} when they may be applied
     */
    public static String reasonToStayOff() {
        List<String> lines;
        try {
            lines = Files.readAllLines(configFile(), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException | LinkageError unreadable) {
            return null;
        }
        String section = "";
        for (String line : lines) {
            Matcher header = SECTION_HEADER.matcher(line);
            if (header.matches()) {
                section = header.group(1).trim();
                continue;
            }
            Matcher entry = BOOLEAN_ENTRY.matcher(line);
            if (!entry.matches()) {
                continue;
            }
            boolean value = Boolean.parseBoolean(entry.group(2));
            if (PATCHES_SECTION.equals(section) && "disablePatches".equals(entry.group(1)) && value) {
                return "flowing_fluid_patches.disablePatches is true";
            }
            if (GENERAL_SECTION.equals(section) && "compatEnabled".equals(entry.group(1)) && !value) {
                return "general.compatEnabled is false";
            }
        }
        return null;
    }

    private static Path configFile() {
        Path configFolder = FMLPaths.CONFIGDIR.get();
        Path current = configFolder.resolve(CONFIG_FILE_NAME);
        return Files.exists(current) ? current : configFolder.resolve(LEGACY_CONFIG_FILE_NAME);
    }
}
