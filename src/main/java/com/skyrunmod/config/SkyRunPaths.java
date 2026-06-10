package com.skyrunmod.config;

import java.nio.file.Path;

import net.fabricmc.loader.api.FabricLoader;

/** Resolves on-disk locations for SkyRunMod under the Fabric config directory. */
public final class SkyRunPaths {
    private SkyRunPaths() {
    }

    /** {@code <.minecraft>/config/skyrunmod}. */
    public static Path configDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("skyrunmod");
    }

    public static Path personalBests() {
        return configDir().resolve("personal_bests.json");
    }

    public static Path settings() {
        return configDir().resolve("settings.json");
    }
}
