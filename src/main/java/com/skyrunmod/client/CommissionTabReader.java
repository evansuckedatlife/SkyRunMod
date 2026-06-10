package com.skyrunmod.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.skyrunmod.core.CommissionParser;
import com.skyrunmod.core.SkyRunAnalyticsEngine;
import com.skyrunmod.core.SkyRunAnalyticsEngine.Activity;
import com.skyrunmod.util.TextUtil;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

/**
 * Polls the player-list (TAB) every half second, extracts the Hypixel commission widget and feeds it
 * to the core {@link com.skyrunmod.core.CommissionTracker}, then mirrors live progress into the
 * overlay. The tab is server-driven, so this works whether or not the player is holding TAB.
 */
public final class CommissionTabReader {
    private static final int POLL_INTERVAL_TICKS = 10; // ~0.5s at 20 TPS

    private static int tickCounter;

    private CommissionTabReader() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(CommissionTabReader::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        if (++tickCounter % POLL_INTERVAL_TICKS != 0) {
            return;
        }
        SkyRunState state = SkyRunState.get();
        if (state == null || client.player == null) {
            return;
        }
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) {
            return;
        }

        List<String> lines = collectTabLines(handler);
        // No commission header → not in a mining area (or a transient tab frame); leave timers alone
        // rather than risk cancelling an in-progress commission.
        if (!CommissionParser.hasHeader(lines)) {
            return;
        }

        Map<String, Double> snapshot = CommissionParser.parse(lines);
        String actor = client.getSession().getUsername();
        long now = state.now();

        state.commissionTracker().update(snapshot, actor, now);
        updateOverlay(state.engine(), snapshot, now);
    }

    private static List<String> collectTabLines(ClientPlayNetworkHandler handler) {
        List<String> lines = new ArrayList<>();
        for (PlayerListEntry entry : handler.getListedPlayerListEntries()) {
            Text displayName = entry.getDisplayName();
            if (displayName == null) {
                continue;
            }
            String clean = TextUtil.clean(displayName.getString());
            if (!clean.isEmpty()) {
                lines.add(clean);
            }
        }
        return lines;
    }

    private static void updateOverlay(SkyRunAnalyticsEngine engine, Map<String, Double> snapshot, long now) {
        SkyRunAnalyticsEngine.OverlayModel overlay = engine.overlayModel();
        // Mark commission context (clears unrelated sections only on a real activity change). Done
        // after the tracker update so a freshly-started commission's section isn't wiped.
        overlay.setActivity(Activity.COMMISSION);
        for (Map.Entry<String, Double> entry : snapshot.entrySet()) {
            double fraction = entry.getValue();
            String text = fraction >= 1.0d
                    ? entry.getKey() + ": DONE"
                    : String.format("%s: %.1f%%", entry.getKey(), fraction * 100.0d);
            overlay.setSection("comm:" + entry.getKey(), text, now);
        }
    }
}
