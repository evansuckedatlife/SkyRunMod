package com.skyrunmod.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TextUtilTest {

    @Test
    void stripsLegacyFormattingCodes() {
        assertEquals("Alice picked up an energy crystal!",
                TextUtil.clean("§r§b§lAlice §r§7picked up an energy crystal!"));
    }

    @Test
    void stripsHexColorSequence() {
        // §x§f§f§5§5§5§5Bob  -> Bob
        assertEquals("Bob", TextUtil.clean("§x§f§f§5§5§5§5Bob"));
    }

    @Test
    void collapsesWhitespaceAndTrims() {
        assertEquals("a b c", TextUtil.clean("  a   b\tc  "));
    }

    @Test
    void nullBecomesEmpty() {
        assertEquals("", TextUtil.clean(null));
    }
}
