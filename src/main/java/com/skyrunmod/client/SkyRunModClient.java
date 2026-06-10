package com.skyrunmod.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/**
 * SkyRunMod client entrypoint. Boots shared state, then registers chat parsing, the HUD overlay,
 * keybinds and the {@code /skyrun} command. Personal bests are flushed to disk both when leaving a
 * server (e.g. exiting a dungeon back to a lobby, or disconnecting from Hypixel) and on client
 * shutdown, so a crash can lose at most the current session's unsaved improvements.
 */
public final class SkyRunModClient implements ClientModInitializer {
    public static final String MOD_ID = "skyrunmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        SkyRunState.bootstrap();

        ChatListener.register();
        SplitOverlay.register();
        SkyRunKeybinds.register();
        SkyRunCommand.register();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> persistQuietly());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> persistQuietly());

        LOGGER.info("SkyRunMod initialized (Skyblock speedrun splits & analytics)");
    }

    private static void persistQuietly() {
        SkyRunState state = SkyRunState.get();
        if (state != null) {
            state.persist();
        }
    }
}
