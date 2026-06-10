package com.skyrunmod.core;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recognizes Kuudra fight signals. The total run split is keyed by tier; the fight start is detected
 * from the Kuudra boss bar (client side), completion from chat. Minecraft-free, unit tested.
 */
public final class KuudraParser {
    private static final Pattern TIER = Pattern.compile("(?i)Tier\\s+([IVX]+)");
    private static final String[] ROMAN = {"I", "II", "III", "IV", "V"};

    private KuudraParser() {
    }

    /** Tier 1–5 from a sidebar line like {@code "Tier IV"}, or 0 if unknown. */
    public static int parseTier(List<String> sidebarLines) {
        if (sidebarLines == null) {
            return 0;
        }
        for (String line : sidebarLines) {
            if (line == null) {
                continue;
            }
            Matcher m = TIER.matcher(line);
            if (m.find()) {
                String roman = m.group(1).toUpperCase();
                for (int i = 0; i < ROMAN.length; i++) {
                    if (ROMAN[i].equals(roman)) {
                        return i + 1;
                    }
                }
            }
        }
        return 0;
    }

    /** True if the boss-bar name is Kuudra's. */
    public static boolean isKuudraBossBar(String bossBarName) {
        return bossBarName != null && bossBarName.toLowerCase().contains("kuudra");
    }

    /** The fight-complete chat broadcast. */
    public static boolean isComplete(String line) {
        return line != null && line.toUpperCase().contains("KUUDRA DOWN");
    }
}
