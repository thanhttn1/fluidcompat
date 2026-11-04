package me.thanhtnt2.fluidcompat.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Carries the player's settings over from the config files of versions that still used the old mod id.
 *
 * <p>The config file names come from the mod id, so after the id changed every player would silently start again
 * with default settings. This copies each old file to its new name once, before NeoForge loads the configs. The old
 * file is left where it is, and a new file that already exists is never overwritten.
 */
public final class LegacyConfigMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger("FluidCompat/Config");

    /** Old name first, new name second. */
    private static final String[][] RENAMED_FILES = {
            {"fluidcompact-common.toml", "fluidcompat-common.toml"},
            {"fluidcompact-compat.toml", "fluidcompat-compat.toml"},
    };

    private LegacyConfigMigration() {
    }

    public static void copyOldSettingsIfNeeded() {
        try {
            Path configFolder = FMLPaths.CONFIGDIR.get();
            for (String[] names : RENAMED_FILES) {
                copyIfOnlyTheOldOneExists(configFolder.resolve(names[0]), configFolder.resolve(names[1]));
            }
        } catch (RuntimeException | LinkageError unexpected) {
            // Settings are a convenience: a failure here must never stop the game from starting
            LOGGER.warn("Could not look for old Fluid Compat settings", unexpected);
        }
    }

    private static void copyIfOnlyTheOldOneExists(Path oldFile, Path newFile) {
        if (Files.exists(newFile) || !Files.isRegularFile(oldFile)) {
            return;
        }
        try {
            Files.copy(oldFile, newFile);
            LOGGER.info("Copied your old settings {} to {}", oldFile.getFileName(), newFile.getFileName());
        } catch (IOException | RuntimeException failure) {
            LOGGER.warn("Could not copy {} to {}, default settings will be used", oldFile.getFileName(), newFile.getFileName(), failure);
        }
    }
}
