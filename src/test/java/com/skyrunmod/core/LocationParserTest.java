package com.skyrunmod.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class LocationParserTest {

    @Test
    void extractsAreaAfterGlyph() {
        List<String> sidebar = List.of(
                "SKYBLOCK",
                "11/06/26",
                "⏣ Crystal Hollows",
                "Powder: 1,234");
        assertEquals("Crystal Hollows", LocationParser.parseArea(sidebar).orElseThrow());
    }

    @Test
    void emptyWhenNoGlyph() {
        assertTrue(LocationParser.parseArea(List.of("SKYBLOCK", "Nothing here")).isEmpty());
        assertTrue(LocationParser.parseArea(List.of()).isEmpty());
    }

    @Test
    void handlesGlyphWithoutSpace() {
        assertEquals("Dwarven Mines", LocationParser.parseArea(List.of("⏣Dwarven Mines")).orElseThrow());
    }
}
