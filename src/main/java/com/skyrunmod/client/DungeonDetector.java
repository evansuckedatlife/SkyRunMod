package com.skyrunmod.client;

import com.skyrunmod.client.hud.ScoreboardReader;
import com.skyrunmod.core.DungeonRunParser;
import com.skyrunmod.core.DungeonRunTracker;
import com.skyrunmod.util.TextUtil;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Drives {@link DungeonRunTracker} from Catacombs chat milestones, with the floor id polled from the
 * sidebar. Run start → blood door → boss entry → run complete.
 */
public final class DungeonDetector {
    private static final int POLL_INTERVAL_TICKS = 20;

    private static int tickCounter;
    private static String currentFloor = "";

    private DungeonDetector() {
    }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register(DungeonDetector::onChat);
        ClientTickEvents.END_CLIENT_TICK.register(DungeonDetector::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        if (++tickCounter % POLL_INTERVAL_TICKS != 0 || client.world == null) {
            return;
        }
        currentFloor = DungeonRunParser.parseFloor(ScoreboardReader.sidebarLines()).orElse("");
    }

    private static void onChat(Text message, boolean overlay) {
        if (overlay) {
            return;
        }
        SkyRunState state = SkyRunState.get();
        if (state == null || !state.config().enableDungeon) {
            return;
        }
        String clean = TextUtil.clean(message.getString());
        if (clean.isEmpty()) {
            return;
        }
        DungeonRunTracker tracker = state.dungeonTracker();
        String actor = actor();
        long now = state.now();

        if (DungeonRunParser.isDungeonStart(clean) && !currentFloor.isEmpty()) {
            tracker.onStart(currentFloor, now);
        } else if (DungeonRunParser.isBloodOpen(clean)) {
            tracker.onBlood(actor, now);
        } else if (DungeonRunParser.isBossEntry(clean)) {
            tracker.onBoss(actor, now);
        } else if (DungeonRunParser.isComplete(clean)) {
            if (tracker.isRunning()) {
                state.sessionRates().record("dungeon_runs", 1, now);
            }
            tracker.onComplete(actor, now);
        }
    }

    private static String actor() {
        return MinecraftClient.getInstance().getSession().getUsername();
    }
}
