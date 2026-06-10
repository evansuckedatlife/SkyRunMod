package com.skyrunmod.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MiningTrackerTest {

    @Test
    void parsesMiningChat() {
        assertEquals("jade", MiningParser.parseCrystal("You found a Jade Crystal!").orElseThrow());
        assertEquals("amethyst", MiningParser.parseCrystal("Placed Amethyst Crystal in the nucleus").orElseThrow());
        assertTrue(MiningParser.parseCrystal("Jade is shiny").isEmpty());
        assertTrue(MiningParser.isMineshaft("You found a Mineshaft!"));
        assertTrue(MiningParser.isCorpse("You looted a Lapis Corpse."));
    }

    @Test
    void nucleusRunTimesFirstToFifth() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        MiningTracker tracker = new MiningTracker(engine);

        tracker.onCrystal("jade", "p", 0L);        // anchor (no split)
        tracker.onCrystal("amber", "p", 10_000L);
        tracker.onCrystal("topaz", "p", 20_000L);
        tracker.onCrystal("sapphire", "p", 30_000L);
        tracker.onCrystal("amethyst", "p", 40_000L); // 5th -> all5

        assertEquals(10_000L, engine.personalBestMillis("mining.nucleus.amber").orElseThrow());
        assertEquals(40_000L, engine.personalBestMillis("mining.nucleus.all5").orElseThrow());
    }

    @Test
    void duplicateCrystalIgnoredWithinRun() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        MiningTracker tracker = new MiningTracker(engine);
        tracker.onCrystal("jade", "p", 0L);
        tracker.onCrystal("jade", "p", 5_000L); // duplicate
        tracker.onCrystal("amber", "p", 8_000L);
        assertEquals(8_000L, engine.personalBestMillis("mining.nucleus.amber").orElseThrow());
    }

    @Test
    void mineshaftGapTimed() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        MiningTracker tracker = new MiningTracker(engine);
        tracker.onMineshaft("p", 0L);          // first — no split
        tracker.onMineshaft("p", 120_000L);    // gap 2 min
        assertEquals(120_000L, engine.personalBestMillis("mining.mineshaft.gap").orElseThrow());
    }
}
