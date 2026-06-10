package com.skyrunmod.core;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Recognizes sea-creature spawn chat lines. Each creature has a distinctive spawn phrase; this maps a
 * curated subset of the well-known ones to a key. Minecraft-free, unit tested.
 *
 * <p>The list is intentionally easy to extend in one place — capture any missing spawn line in-game
 * with {@code /skyrun actionbar}/chat and add a fragment here.
 */
public final class FishingParser {
    /** lower-case fragment → creature key. Insertion order = match priority. */
    private static final Map<String, String> SPAWN_FRAGMENTS = new LinkedHashMap<>();

    static {
        SPAWN_FRAGMENTS.put("a squid appeared", "squid");
        SPAWN_FRAGMENTS.put("you caught a sea walker", "sea_walker");
        SPAWN_FRAGMENTS.put("sea guardian", "sea_guardian");
        SPAWN_FRAGMENTS.put("sea witch", "sea_witch");
        SPAWN_FRAGMENTS.put("sea archer", "sea_archer");
        SPAWN_FRAGMENTS.put("night squid", "night_squid");
        SPAWN_FRAGMENTS.put("frozen steve", "frozen_steve");
        SPAWN_FRAGMENTS.put("grim reaper", "grim_reaper");
        SPAWN_FRAGMENTS.put("water hydra", "water_hydra");
        SPAWN_FRAGMENTS.put("sea emperor", "sea_emperor");
        SPAWN_FRAGMENTS.put("a yeti", "yeti");
        SPAWN_FRAGMENTS.put("reindrake", "reindrake");
        SPAWN_FRAGMENTS.put("great white shark", "great_white_shark");
        SPAWN_FRAGMENTS.put("blue ringed octopus", "blue_ringed_octopus");
        SPAWN_FRAGMENTS.put("flaming worm", "flaming_worm");
        SPAWN_FRAGMENTS.put("lava blaze", "lava_blaze");
        SPAWN_FRAGMENTS.put("lava pigman", "lava_pigman");
    }

    private FishingParser() {
    }

    /** The sea-creature key if this line is a known spawn announcement. */
    public static Optional<String> parseSeaCreature(String line) {
        if (line == null) {
            return Optional.empty();
        }
        String lower = line.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : SPAWN_FRAGMENTS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }
}
