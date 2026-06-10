package com.skyrunmod.client;

import com.skyrunmod.core.CommissionParser;
import com.skyrunmod.util.TextUtil;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Bridges Hypixel's server chat into the analytics engine. Hypixel sends its split-driving lines
 * (crystal pickups, terminal/device/lever completions, dragon kills, relic pickups) as <em>game</em>
 * messages, so {@link ClientReceiveMessageEvents#GAME} is the right hook.
 */
public final class ChatListener {
    private ChatListener() {
    }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register(ChatListener::onGameMessage);
    }

    private static void onGameMessage(Text message, boolean overlay) {
        // Action-bar (overlay) spam isn't chat content we parse; skip it to avoid false positives.
        if (overlay) {
            return;
        }
        SkyRunState state = SkyRunState.get();
        if (state == null) {
            return;
        }
        String clean = TextUtil.clean(message.getString());
        if (clean.isEmpty()) {
            return;
        }
        try {
            state.engine().onChatMessage(clean, state.now());
            // Commission completion broadcast is the precise, instant signal — prefer it over the
            // tab-list DONE poll. completeByName is a no-op if the tab already completed it.
            CommissionParser.parseCompletionName(clean).ifPresent(name ->
                    state.commissionTracker().completeByName(name, localPlayerName(), state.now()));
        } catch (RuntimeException e) {
            // Never let a parsing edge case break the vanilla chat pipeline.
            SkyRunModClient.LOGGER.warn("SkyRunMod chat parse error for line '{}'", clean, e);
        }
    }

    private static String localPlayerName() {
        return MinecraftClient.getInstance().getSession().getUsername();
    }
}
