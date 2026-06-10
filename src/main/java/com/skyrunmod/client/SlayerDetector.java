package com.skyrunmod.client;

import java.util.Optional;

import com.skyrunmod.client.hud.ScoreboardReader;
import com.skyrunmod.core.SlayerParser;
import com.skyrunmod.core.SlayerTracker;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Drives {@link SlayerTracker} from the sidebar (quest start / boss spawn / quest end) plus the
 * precise quest-complete chat broadcast. Mirrors the commission design: a polled snapshot for state
 * transitions, with chat as the frame-accurate completion signal.
 */
public final class SlayerDetector {
    private static final int POLL_INTERVAL_TICKS = 10; // ~0.5s
    private static final int ABSENCE_TICKS_TO_END = 2; // ~1s of no slayer sidebar => quest ended

    private static int tickCounter;
    private static int absenceCount;

    private SlayerDetector() {
    }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register(SlayerDetector::onChat);
        ClientTickEvents.END_CLIENT_TICK.register(SlayerDetector::onClientTick);
    }

    private static void onChat(Text message, boolean overlay) {
        if (overlay) {
            return;
        }
        SkyRunState state = SkyRunState.get();
        if (state == null || !state.config().enableSlayer) {
            return;
        }
        String clean = com.skyrunmod.util.TextUtil.clean(message.getString());
        if (SlayerParser.isQuestComplete(clean)) {
            long now = state.now();
            state.slayerTracker().onComplete(actor(), now);
            state.sessionRates().record("slayer_bosses", 1, now);
        }
    }

    private static void onClientTick(MinecraftClient client) {
        if (++tickCounter % POLL_INTERVAL_TICKS != 0) {
            return;
        }
        SkyRunState state = SkyRunState.get();
        if (state == null || client.player == null || !state.config().enableSlayer) {
            return;
        }
        SlayerTracker tracker = state.slayerTracker();
        long now = state.now();
        Optional<SlayerParser.Snapshot> parsed = SlayerParser.parseSnapshot(ScoreboardReader.sidebarLines());

        if (parsed.isEmpty()) {
            // Require sustained absence so a transient sidebar frame doesn't end a live quest.
            if (tracker.isActive() && ++absenceCount >= ABSENCE_TICKS_TO_END) {
                if (tracker.isBossUp()) {
                    tracker.onComplete(actor(), now); // chat may have missed it
                } else {
                    tracker.onAbort();
                }
                absenceCount = 0;
            }
            return;
        }
        absenceCount = 0;
        SlayerParser.Snapshot snapshot = parsed.get();

        if (!tracker.isActive()) {
            // Only begin timing a quest we caught during the grind (avoids a 0ms toSpawn split when
            // we join mid-boss); type must be known to key the splits.
            if (snapshot.type() != null && !snapshot.bossActive()) {
                tracker.onQuestStart(snapshot.type(), snapshot.tier(), now);
            }
            return;
        }
        if (!tracker.isBossUp() && snapshot.bossActive()) {
            tracker.onBossSpawn(actor(), now);
        }
    }

    private static String actor() {
        return MinecraftClient.getInstance().getSession().getUsername();
    }
}
