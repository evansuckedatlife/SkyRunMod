package com.skyrunmod.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SessionRatesTest {

    @Test
    void countsAndProjectsPerHour() {
        SessionRates rates = new SessionRates();
        rates.record("mineshafts", 1, 0L);          // session anchors here
        rates.record("mineshafts", 1, 30 * 60_000L);
        rates.record("mineshafts", 1, 60 * 60_000L); // 3 over 1 hour

        assertEquals(3, rates.count("mineshafts"));
        assertEquals(3.0d, rates.perHour("mineshafts", 60 * 60_000L), 1e-6);
    }

    @Test
    void unknownMetricIsZeroAndResetClears() {
        SessionRates rates = new SessionRates();
        assertEquals(0, rates.count("nothing"));
        assertEquals(0d, rates.perHour("nothing", 1000L), 1e-9);

        rates.record("x", 5, 0L);
        assertEquals(5, rates.count("x"));
        rates.reset();
        assertTrue(rates.isEmpty());
        assertEquals(0, rates.count("x"));
    }
}
