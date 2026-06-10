package com.skyrunmod.core;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recognizes Catacombs run milestones from chat + the floor from the sidebar. Minecraft-free, unit
 * tested. Loose matching (substrings) so minor wording changes don't break it; verify in-game with
 * {@code /skyrun scoreboard}.
 */
public final class DungeonRunParser {
    private static final Pattern FLOOR = Pattern.compile("\\(([FM]\\d+)\\)");
    private static final Pattern BOSS_LINE = Pattern.compile("^\\[BOSS] .+");

    private DungeonRunParser() {
    }

    /** Floor id like {@code F7} / {@code M7} from a sidebar that shows {@code The Catacombs (F7)}. */
    public static Optional<String> parseFloor(List<String> sidebarLines) {
        if (sidebarLines == null) {
            return Optional.empty();
        }
        for (String line : sidebarLines) {
            if (line == null) {
                continue;
            }
            Matcher m = FLOOR.matcher(line);
            if (m.find()) {
                return Optional.of(m.group(1));
            }
        }
        return Optional.empty();
    }

    public static boolean isDungeonStart(String line) {
        return line != null && line.contains("Dungeon starts in");
    }

    public static boolean isBloodOpen(String line) {
        return line != null && line.toUpperCase().contains("BLOOD DOOR HAS BEEN OPENED");
    }

    /** First {@code [BOSS] …} line of a run marks boss-room entry. */
    public static boolean isBossEntry(String line) {
        return line != null && BOSS_LINE.matcher(line).matches();
    }

    /** The end-of-run summary header ({@code > EXTRA STATS <}) is a distinctive completion signal. */
    public static boolean isComplete(String line) {
        return line != null && line.toUpperCase().contains("EXTRA STATS");
    }
}
