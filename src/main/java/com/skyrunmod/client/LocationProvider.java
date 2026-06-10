package com.skyrunmod.client;

import com.skyrunmod.client.hud.ScoreboardReader;
import com.skyrunmod.core.LocationParser;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

/**
 * Tracks the current SkyBlock area (from the sidebar) once a second, so activity detectors can gate
 * on location and avoid cross-activity false positives.
 */
public final class LocationProvider {
    private static volatile String currentArea = "";
    private static int tickCounter;

    private LocationProvider() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(LocationProvider::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        if (++tickCounter % 20 != 0) {
            return;
        }
        if (client.world == null) {
            currentArea = "";
            return;
        }
        currentArea = LocationParser.parseArea(ScoreboardReader.sidebarLines()).orElse("");
    }

    /** Current area name (e.g. "Crystal Hollows"), or empty if unknown / not on SkyBlock. */
    public static String currentArea() {
        return currentArea;
    }

    /** True if the current area name contains {@code token} (case-insensitive). */
    public static boolean areaContains(String token) {
        return currentArea.toLowerCase().contains(token.toLowerCase());
    }
}
