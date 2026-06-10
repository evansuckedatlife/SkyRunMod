package com.skyrunmod.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interprets the SkyBlock sidebar (and a couple of chat lines) for an active Slayer quest.
 * Minecraft-free and unit tested; {@link com.skyrunmod.client.SlayerDetector} feeds it cleaned
 * scoreboard lines and drives {@link SlayerTracker} from the transitions.
 *
 * <p>Exact Hypixel wording is matched loosely (substring/known boss names) so minor server-side
 * changes don't silently break detection; verify in-game with {@code /skyrun scoreboard}.
 */
public final class SlayerParser {
    /** Boss-name keyword (as shown) → short type key. Insertion order = match priority. */
    private static final Map<String, String> TYPE_BY_KEYWORD = new LinkedHashMap<>();

    static {
        TYPE_BY_KEYWORD.put("Revenant", "revenant");
        TYPE_BY_KEYWORD.put("Tarantula", "tarantula");
        TYPE_BY_KEYWORD.put("Sven", "sven");
        TYPE_BY_KEYWORD.put("Voidgloom", "voidgloom");
        TYPE_BY_KEYWORD.put("Inferno", "inferno");
        TYPE_BY_KEYWORD.put("Bloodfiend", "bloodfiend");
    }

    private SlayerParser() {
    }

    /** Parsed slayer state from the sidebar; empty when no slayer quest is shown. */
    public static Optional<Snapshot> parseSnapshot(List<String> sidebarLines) {
        if (sidebarLines == null) {
            return Optional.empty();
        }
        String type = null;
        int tier = 0;
        boolean bossActive = false;
        boolean header = false;

        for (String raw : sidebarLines) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            String lower = line.toLowerCase();

            if (lower.contains("slayer quest") || lower.contains("slay the boss")) {
                header = true;
            }
            if (lower.contains("slay the boss") || line.contains("❤") || lower.contains("boss slain")) {
                bossActive = true;
            }
            if (type == null) {
                for (Map.Entry<String, String> e : TYPE_BY_KEYWORD.entrySet()) {
                    if (line.contains(e.getKey())) {
                        type = e.getValue();
                        int parsed = parseTier(line);
                        if (parsed > 0) {
                            tier = parsed;
                        }
                        break;
                    }
                }
            }
        }

        if (!header && type == null) {
            return Optional.empty();
        }
        return Optional.of(new Snapshot(type, tier, bossActive));
    }

    /** True if this chat line is the slayer quest-start broadcast. */
    public static boolean isQuestStarted(String cleanedLine) {
        return cleanedLine != null && cleanedLine.toUpperCase().contains("SLAYER QUEST STARTED");
    }

    /** True if this chat line is the slayer quest-complete broadcast (the boss was slain). */
    public static boolean isQuestComplete(String cleanedLine) {
        if (cleanedLine == null) {
            return false;
        }
        String upper = cleanedLine.toUpperCase();
        return upper.contains("SLAYER QUEST COMPLETE") || upper.contains("SLAYER BOSS SLAIN");
    }

    /** Finds a standalone tier roman numeral (I–V) in a line, else 0. */
    private static int parseTier(String line) {
        for (String token : line.split("\\s+")) {
            switch (token) {
                case "I": return 1;
                case "II": return 2;
                case "III": return 3;
                case "IV": return 4;
                case "V": return 5;
                default: break;
            }
        }
        return 0;
    }

    /**
     * Slayer sidebar state.
     *
     * @param type       short type key (revenant/tarantula/sven/voidgloom/inferno/bloodfiend), or
     *                   {@code null} if the boss name isn't on the sidebar this frame
     * @param tier       1–5, or 0 if unknown
     * @param bossActive whether the boss is currently up (vs still grinding XP to spawn it)
     */
    public record Snapshot(String type, int tier, boolean bossActive) {
    }
}
