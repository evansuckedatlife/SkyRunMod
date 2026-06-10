package com.skyrunmod.core;

import java.util.Locale;
import java.util.Optional;

/**
 * Recognizes mining chat lines: Crystal Hollows nucleus crystals, Mineshaft discoveries, corpse
 * loot. Minecraft-free, unit tested. Loose substring matching; verify exact wording in-game.
 */
public final class MiningParser {
    private static final String[] CRYSTALS = {"Jade", "Amber", "Topaz", "Sapphire", "Amethyst"};

    private MiningParser() {
    }

    /** If a Crystal Hollows nucleus crystal was obtained/placed, the lower-case crystal key. */
    public static Optional<String> parseCrystal(String line) {
        if (line == null) {
            return Optional.empty();
        }
        String lower = line.toLowerCase(Locale.ROOT);
        if (!lower.contains("crystal")) {
            return Optional.empty();
        }
        boolean gained = lower.contains("found") || lower.contains("placed") || lower.contains("obtained");
        if (!gained) {
            return Optional.empty();
        }
        for (String crystal : CRYSTALS) {
            if (line.contains(crystal)) {
                return Optional.of(crystal.toLowerCase(Locale.ROOT));
            }
        }
        return Optional.empty();
    }

    public static boolean isMineshaft(String line) {
        if (line == null) {
            return false;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("mineshaft")
                && (lower.contains("found") || lower.contains("enter") || lower.contains("discover"));
    }

    public static boolean isCorpse(String line) {
        return line != null && line.toLowerCase(Locale.ROOT).contains("corpse");
    }
}
