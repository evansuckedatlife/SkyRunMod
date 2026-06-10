package com.skyrunmod.client.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.skyrunmod.util.TextUtil;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;

/**
 * Reads the SkyBlock sidebar (scoreboard) as cleaned text lines, top to bottom.
 *
 * <p>Hypixel encodes each visible line in the score-holder's team prefix + suffix (the holder name
 * itself is a throwaway colour token), so we concatenate prefix+suffix per entry and order by score
 * value descending to match the on-screen order.
 */
public final class ScoreboardReader {
    private ScoreboardReader() {
    }

    /** Cleaned sidebar lines, top to bottom. Empty if no sidebar is shown. */
    public static List<String> sidebarLines() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return List.of();
        }
        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) {
            return List.of();
        }

        List<ScoreboardEntry> entries = new ArrayList<>(scoreboard.getScoreboardEntries(objective));
        entries.sort(Comparator.comparingInt(ScoreboardEntry::value).reversed());

        List<String> lines = new ArrayList<>(entries.size());
        for (ScoreboardEntry entry : entries) {
            Team team = scoreboard.getScoreHolderTeam(entry.owner());
            String raw = team != null
                    ? team.getPrefix().getString() + team.getSuffix().getString()
                    : entry.owner();
            String clean = TextUtil.clean(raw);
            if (!clean.isEmpty()) {
                lines.add(clean);
            }
        }
        return lines;
    }

    /** The sidebar title (e.g. {@code SKYBLOCK}), cleaned. Empty if no sidebar is shown. */
    public static String title() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return "";
        }
        ScoreboardObjective objective =
                client.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        return objective == null ? "" : TextUtil.clean(objective.getDisplayName().getString());
    }
}
