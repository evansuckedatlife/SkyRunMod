package com.skyrunmod.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DianaTrackerTest {

    @Test
    void recognizesChatLines() {
        assertTrue(DianaParser.isBurrowDig("You dug out a Griffin Burrow! (2/4)"));
        assertTrue(DianaParser.isInquisitor("Wow! You dug out a Minos Inquisitor!"));
        assertFalse(DianaParser.isBurrowDig("Wow! You dug out a Minos Inquisitor!"), "inquisitor isn't a burrow");
    }

    @Test
    void timesGapBetweenConsecutiveDigs() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        DianaTracker tracker = new DianaTracker(engine);

        tracker.onBurrowDig("p", 0L);       // chain start — no split
        assertTrue(engine.recentSplits().isEmpty());
        tracker.onBurrowDig("p", 8_000L);   // gap of 8s
        assertEquals(8_000L, engine.personalBestMillis("diana.burrow.gap").orElseThrow());
    }

    @Test
    void longGapStartsNewChainInsteadOfOneSlowStep() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        DianaTracker tracker = new DianaTracker(engine);

        tracker.onBurrowDig("p", 0L);
        tracker.onBurrowDig("p", DianaTracker.CHAIN_RESET_MILLIS + 5_000L); // too long → new chain
        assertTrue(engine.recentSplits().isEmpty(), "no gap split across a reset boundary");

        tracker.onBurrowDig("p", DianaTracker.CHAIN_RESET_MILLIS + 9_000L); // 4s into the new chain
        assertEquals(4_000L, engine.personalBestMillis("diana.burrow.gap").orElseThrow());
    }

    @Test
    void inquisitorTimedFromChainStart() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        DianaTracker tracker = new DianaTracker(engine);

        tracker.onBurrowDig("p", 0L);       // chain start
        tracker.onBurrowDig("p", 5_000L);
        tracker.onInquisitor("p", 12_000L); // 12s from chain start
        assertEquals(12_000L, engine.personalBestMillis("diana.inquisitor").orElseThrow());
    }
}
