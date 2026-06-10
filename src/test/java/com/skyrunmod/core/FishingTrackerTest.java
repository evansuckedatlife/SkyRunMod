package com.skyrunmod.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FishingTrackerTest {

    @Test
    void parsesKnownSeaCreatures() {
        assertEquals("squid", FishingParser.parseSeaCreature("A Squid appeared.").orElseThrow());
        assertEquals("sea_emperor",
                FishingParser.parseSeaCreature("The Sea Emperor arises from the depths!").orElseThrow());
        assertTrue(FishingParser.parseSeaCreature("just a normal fish").isEmpty());
    }

    @Test
    void timesGapBetweenCatchesAndCounts() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        FishingTracker tracker = new FishingTracker(engine);

        tracker.onSeaCreature("squid", "p", 0L);          // first — no gap split
        assertEquals(1, tracker.catches());
        tracker.onSeaCreature("squid", "p", 25_000L);     // 25s gap
        assertEquals(2, tracker.catches());
        assertEquals(25_000L, engine.personalBestMillis("fishing.seacreature.gap").orElseThrow());
    }

    @Test
    void longIdleBreaksTheStreak() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        FishingTracker tracker = new FishingTracker(engine);
        tracker.onSeaCreature("squid", "p", 0L);
        tracker.onSeaCreature("squid", "p", FishingTracker.SESSION_RESET_MILLIS + 10_000L);
        assertTrue(engine.recentSplits().isEmpty(), "no gap split across an idle break");
    }
}
