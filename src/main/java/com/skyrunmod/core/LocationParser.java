package com.skyrunmod.core;

import java.util.List;
import java.util.Optional;

/**
 * Extracts the current SkyBlock area from cleaned sidebar lines. The area line is marked by the
 * SkyBlock area glyph {@code ⏣} (U+23E3), e.g. {@code "⏣ Crystal Hollows"}. Minecraft-free, unit tested.
 */
public final class LocationParser {
    private static final char AREA_SYMBOL = '⏣';

    private LocationParser() {
    }

    /** The area name following the {@code ⏣} glyph, if present in any sidebar line. */
    public static Optional<String> parseArea(List<String> sidebarLines) {
        if (sidebarLines == null) {
            return Optional.empty();
        }
        for (String line : sidebarLines) {
            if (line == null) {
                continue;
            }
            int idx = line.indexOf(AREA_SYMBOL);
            if (idx >= 0) {
                String area = line.substring(idx + 1).trim();
                if (!area.isEmpty()) {
                    return Optional.of(area);
                }
            }
        }
        return Optional.empty();
    }
}
