package com.skyrunmod.client;

import com.skyrunmod.core.FishingParser;
import com.skyrunmod.core.MiningParser;
import com.skyrunmod.util.TextUtil;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Routes chat to the {@link com.skyrunmod.core.MiningTracker} (nucleus crystals, mineshafts) and
 * {@link com.skyrunmod.core.FishingTracker} (sea-creature spawns). Both are pure chat detectors, so
 * they share one listener.
 */
public final class MiningFishingDetector {
    private MiningFishingDetector() {
    }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register(MiningFishingDetector::onChat);
    }

    private static void onChat(Text message, boolean overlay) {
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
        String actor = actor();
        long now = state.now();

        if (state.config().enableMining) {
            MiningParser.parseCrystal(clean).ifPresent(crystal -> {
                state.miningTracker().onCrystal(crystal, actor, now);
                state.sessionRates().record("crystals", 1, now);
            });
            if (MiningParser.isMineshaft(clean)) {
                state.miningTracker().onMineshaft(actor, now);
                state.sessionRates().record("mineshafts", 1, now);
            }
        }
        if (state.config().enableFishing) {
            FishingParser.parseSeaCreature(clean).ifPresent(creature -> {
                state.fishingTracker().onSeaCreature(creature, actor, now);
                state.sessionRates().record("sea_creatures", 1, now);
            });
        }
    }

    private static String actor() {
        return MinecraftClient.getInstance().getSession().getUsername();
    }
}
