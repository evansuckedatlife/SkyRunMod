package com.skyrunmod.util;

import java.util.regex.Pattern;

/** String hygiene for chat parsing. Operates on plain strings (no Minecraft types). */
public final class TextUtil {
    // Section-sign formatting codes (§a, §l, ...) plus the hex prefix §x used by the
    // §x§r§r§g§g§b§b form; stripping §x and each following §<hex> clears the whole sequence.
    private static final Pattern FORMATTING_CODES = Pattern.compile("(?i)§[0-9A-FK-ORX]");

    private TextUtil() {
    }

    /**
     * Strips legacy section-sign formatting codes and collapses runs of whitespace, so a richly
     * decorated Hypixel line like {@code "§r§b§lAlice §r§7picked up..."} reduces to a clean string the
     * engine's regexes can match.
     */
    public static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String stripped = FORMATTING_CODES.matcher(raw).replaceAll("");
        return stripped.replaceAll("\\s+", " ").trim();
    }
}
