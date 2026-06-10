package com.skyrunmod.core;

/**
 * Recognizes Diana / Mythological Ritual chat lines (burrow digs, inquisitor spawns). Minecraft-free,
 * unit tested. Loose substring matching; verify exact wording in-game.
 */
public final class DianaParser {
    private DianaParser() {
    }

    /** A burrow was dug out (start of a chain or a step along it). */
    public static boolean isBurrowDig(String line) {
        if (line == null) {
            return false;
        }
        String lower = line.toLowerCase();
        return lower.contains("dug out") && lower.contains("burrow");
    }

    /** A Minos Inquisitor was dug out. */
    public static boolean isInquisitor(String line) {
        return line != null && line.toLowerCase().contains("inquisitor");
    }
}
