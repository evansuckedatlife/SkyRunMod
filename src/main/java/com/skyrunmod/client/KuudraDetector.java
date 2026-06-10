package com.skyrunmod.client;

import com.skyrunmod.client.hud.BossBarReader;
import com.skyrunmod.client.hud.ScoreboardReader;
import com.skyrunmod.core.KuudraParser;
import com.skyrunmod.core.KuudraTracker;
import com.skyrunmod.util.TextUtil;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Starts a Kuudra run when the Kuudra boss bar appears and ends it on the {@code KUUDRA DOWN} chat
 * (boss-bar disappearance is a fallback abort). Tier is read from the sidebar.
 */
public final class KuudraDetector {
    private static final int POLL_INTERVAL_TICKS = 10;
    private static final int ABSENCE_TICKS_TO_ABORT = 10; // ~5s without the boss bar

    private static int tickCounter;
    private static int absenceCount;

    private KuudraDetector() {
    }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register(KuudraDetector::onChat);
        ClientTickEvents.END_CLIENT_TICK.register(KuudraDetector::onClientTick);
    }

    private static void onChat(Text message, boolean overlay) {
        if (overlay) {
            return;
        }
        SkyRunState state = SkyRunState.get();
        if (state == null || !state.config().enableKuudra) {
            return;
        }
        if (KuudraParser.isComplete(TextUtil.clean(message.getString()))) {
            long now = state.now();
            if (state.kuudraTracker().isRunning()) {
                state.sessionRates().record("kuudra_runs", 1, now);
            }
            state.kuudraTracker().onComplete(actor(), now);
        }
    }

    private static void onClientTick(MinecraftClient client) {
        if (++tickCounter % POLL_INTERVAL_TICKS != 0 || client.world == null) {
            return;
        }
        SkyRunState state = SkyRunState.get();
        if (state == null || !state.config().enableKuudra) {
            return;
        }
        KuudraTracker tracker = state.kuudraTracker();

        boolean kuudraUp = BossBarReader.activeBossBars().stream()
                .anyMatch(bar -> KuudraParser.isKuudraBossBar(bar.name()));

        if (kuudraUp) {
            absenceCount = 0;
            if (!tracker.isRunning()) {
                int tier = KuudraParser.parseTier(ScoreboardReader.sidebarLines());
                tracker.onStart(tier, state.now());
            }
        } else if (tracker.isRunning() && ++absenceCount >= ABSENCE_TICKS_TO_ABORT) {
            tracker.onAbort();
            absenceCount = 0;
        }
    }

    private static String actor() {
        return MinecraftClient.getInstance().getSession().getUsername();
    }
}
