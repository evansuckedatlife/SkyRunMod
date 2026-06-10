package com.skyrunmod.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TimeFormatTest {

    @Test
    void formatsSubMinuteWithMillis() {
        assertEquals("0.000", TimeFormat.duration(0L));
        assertEquals("4.207", TimeFormat.duration(4207L));
        assertEquals("59.999", TimeFormat.duration(59_999L));
    }

    @Test
    void formatsMinutesAndHours() {
        assertEquals("1:04.207", TimeFormat.duration(64_207L));
        assertEquals("1:00:00.000", TimeFormat.duration(3_600_000L));
        assertEquals("2:03:04.005", TimeFormat.duration((2 * 3600 + 3 * 60 + 4) * 1000L + 5L));
    }

    @Test
    void negativeDurationGetsLeadingSign() {
        assertEquals("-1.500", TimeFormat.duration(-1500L));
    }

    @Test
    void signedDeltaPrefixesSign() {
        assertEquals("+1.204", TimeFormat.signedDelta(1204L));
        assertEquals("-0.300", TimeFormat.signedDelta(-300L));
        assertEquals("-0.000", TimeFormat.signedDelta(0L));
    }
}
