package com.skyrunmod.client.hud;

import java.util.ArrayList;
import java.util.List;

import com.skyrunmod.mixin.BossBarHudAccessor;
import com.skyrunmod.util.TextUtil;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;

/** Reads the client's active boss bars (cleaned name + fill fraction) via {@link BossBarHudAccessor}. */
public final class BossBarReader {
    private BossBarReader() {
    }

    public static List<BossBarInfo> activeBossBars() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null) {
            return List.of();
        }
        BossBarHud hud = client.inGameHud.getBossBarHud();
        if (hud == null) {
            return List.of();
        }
        List<BossBarInfo> result = new ArrayList<>();
        for (ClientBossBar bar : ((BossBarHudAccessor) hud).skyrunmod$getBossBars().values()) {
            String name = TextUtil.clean(bar.getName().getString());
            if (!name.isEmpty()) {
                result.add(new BossBarInfo(name, bar.getPercent()));
            }
        }
        return result;
    }

    /** A single boss bar: cleaned display name and its 0..1 fill fraction. */
    public record BossBarInfo(String name, float percent) {
    }
}
