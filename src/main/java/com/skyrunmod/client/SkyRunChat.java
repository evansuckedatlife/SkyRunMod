package com.skyrunmod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Small helper for sending SkyRunMod's prefixed feedback to the client chat. */
public final class SkyRunChat {
    private SkyRunChat() {
    }

    public static void feedback(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        MutableText prefix = Text.literal("SkyRun ").formatted(Formatting.AQUA, Formatting.BOLD)
                .append(Text.literal("» ").formatted(Formatting.DARK_GRAY));
        Text body = Text.literal(message).formatted(Formatting.GRAY);
        client.player.sendMessage(prefix.append(body), false);
    }
}
