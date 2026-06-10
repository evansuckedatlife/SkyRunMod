package com.skyrunmod.client;

import com.skyrunmod.util.TextUtil;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

/**
 * Captures the action bar (the {@code overlay=true} game-message stream — slayer progress, +XP,
 * powder gains, etc.) that {@link ChatListener} deliberately ignores. Stores the latest cleaned line
 * for the {@code /skyrun actionbar} diagnostic and for activity detectors to poll.
 */
public final class ActionBarListener {
    private static volatile String lastActionBar = "";

    private ActionBarListener() {
    }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) {
                lastActionBar = TextUtil.clean(message.getString());
            }
        });
    }

    /** Most recent action bar text, cleaned of formatting. */
    public static String last() {
        return lastActionBar;
    }
}
