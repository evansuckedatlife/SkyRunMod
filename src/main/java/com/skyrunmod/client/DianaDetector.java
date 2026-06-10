package com.skyrunmod.client;

import com.skyrunmod.core.DianaParser;
import com.skyrunmod.util.TextUtil;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/** Feeds Diana burrow-dig / inquisitor chat lines to {@link com.skyrunmod.core.DianaTracker}. */
public final class DianaDetector {
    private DianaDetector() {
    }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register(DianaDetector::onChat);
    }

    private static void onChat(Text message, boolean overlay) {
        if (overlay) {
            return;
        }
        SkyRunState state = SkyRunState.get();
        if (state == null || !state.config().enableDiana) {
            return;
        }
        String clean = TextUtil.clean(message.getString());
        long now = state.now();
        // Inquisitor first: that line also says "dug out" but isn't a burrow.
        if (DianaParser.isInquisitor(clean)) {
            state.dianaTracker().onInquisitor(actor(), now);
            state.sessionRates().record("inquisitors", 1, now);
        } else if (DianaParser.isBurrowDig(clean)) {
            state.dianaTracker().onBurrowDig(actor(), now);
            state.sessionRates().record("burrows", 1, now);
        }
    }

    private static String actor() {
        return MinecraftClient.getInstance().getSession().getUsername();
    }
}
