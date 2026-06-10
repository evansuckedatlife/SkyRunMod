package com.skyrunmod.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Hypixel's commission widget out of the player-list (TAB) display names.
 *
 * <p>In the Dwarven Mines / Crystal Hollows / Glacite Tunnels, Hypixel renders active commissions as
 * tab entries under a {@code Commissions} header, each line of the form {@code "<name>: <pct>%"} or
 * {@code "<name>: DONE"} once finished. This class is Minecraft-free so it can be unit tested; the
 * client layer is responsible only for extracting the cleaned display strings.
 *
 * <p>Parsing is gated on the presence of the {@code Commissions} header, so it never fires outside
 * mining areas (where an unrelated {@code "X: 50%"} tab line could otherwise be mistaken for one).
 */
public final class CommissionParser {
    private static final Pattern PROGRESS =
            Pattern.compile("^(.+?):\\s*(\\d{1,3}(?:[.,]\\d+)?)%$");
    private static final Pattern DONE =
            Pattern.compile("^(.+?):\\s*DONE$", Pattern.CASE_INSENSITIVE);
    // Chat broadcast on completion, e.g. "LAVA SPRINGS MITHRIL Commission Complete! Visit the King...".
    private static final Pattern COMPLETION =
            Pattern.compile("^(.+?) Commission Complete!.*$", Pattern.CASE_INSENSITIVE);

    private CommissionParser() {
    }

    /**
     * If {@code cleanedLine} is the Hypixel commission-completion chat broadcast, returns the
     * commission name (as printed, typically upper-case). The name should be matched against tracked
     * commissions case-insensitively, since the tab shows it in title case.
     */
    public static Optional<String> parseCompletionName(String cleanedLine) {
        if (cleanedLine == null) {
            return Optional.empty();
        }
        Matcher m = COMPLETION.matcher(cleanedLine.trim());
        return m.matches() ? Optional.of(m.group(1).trim()) : Optional.empty();
    }

    /**
     * Parses commission progress from already-cleaned tab lines.
     *
     * @return an insertion-ordered map of commission name → progress fraction in {@code [0,1]}, where
     *         a completed ({@code DONE}) commission is {@code 1.0}. Empty if no commission header is
     *         present (i.e. not in a mining area).
     */
    public static Map<String, Double> parse(List<String> cleanedLines) {
        if (!hasHeader(cleanedLines)) {
            return Map.of();
        }
        Map<String, Double> result = new LinkedHashMap<>();
        for (String raw : cleanedLines) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();

            Matcher done = DONE.matcher(line);
            if (done.matches()) {
                result.put(done.group(1).trim(), 1.0d);
                continue;
            }

            Matcher progress = PROGRESS.matcher(line);
            if (progress.matches()) {
                double pct = Double.parseDouble(progress.group(2).replace(',', '.'));
                result.put(progress.group(1).trim(), clampFraction(pct / 100.0d));
            }
        }
        return result;
    }

    /**
     * True if the tab snapshot contains the {@code Commissions} header — i.e. the player is in a
     * mining area where commissions are shown. Lets callers distinguish "no commissions because not
     * in a mining area" from "in a mining area with none active".
     */
    public static boolean hasHeader(List<String> cleanedLines) {
        for (String raw : cleanedLines) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            // The header is the only "commission"-bearing line that is not itself a progress entry.
            if (line.toLowerCase().contains("commission")
                    && !PROGRESS.matcher(line).matches()
                    && !DONE.matcher(line).matches()) {
                return true;
            }
        }
        return false;
    }

    private static double clampFraction(double f) {
        if (f < 0d) {
            return 0d;
        }
        return Math.min(f, 1.0d);
    }
}
