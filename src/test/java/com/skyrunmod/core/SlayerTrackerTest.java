package com.skyrunmod.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlayerTrackerTest {

    @Test
    void recordsThreeSplitsAcrossAQuest() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        SlayerTracker tracker = new SlayerTracker(engine);

        tracker.onQuestStart("revenant", 5, 0L);
        assertTrue(tracker.isActive());
        tracker.onBossSpawn("p", 30_000L);   // 30s grind
        assertTrue(tracker.isBossUp());
        tracker.onComplete("p", 50_000L);     // 20s fight, 50s total
        assertFalse(tracker.isActive());

        assertEquals(30_000L, engine.personalBestMillis("slayer.revenant.5.toSpawn").orElseThrow());
        assertEquals(20_000L, engine.personalBestMillis("slayer.revenant.5.fight").orElseThrow());
        assertEquals(50_000L, engine.personalBestMillis("slayer.revenant.5.total").orElseThrow());
        assertEquals(3, engine.recentSplits().size());
    }

    @Test
    void abortCancelsWithoutRecording() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        SlayerTracker tracker = new SlayerTracker(engine);

        tracker.onQuestStart("sven", 4, 0L);
        tracker.onAbort();

        assertFalse(tracker.isActive());
        assertTrue(engine.recentSplits().isEmpty());
        assertFalse(engine.isTracking("slayer.sven.4.total"));
    }

    @Test
    void completeWhileGrindingCancelsGrindButClosesTotal() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        SlayerTracker tracker = new SlayerTracker(engine);

        // Boss-spawn frame was missed; completion arrives during GRINDING.
        tracker.onQuestStart("inferno", 3, 0L);
        tracker.onComplete("p", 40_000L);

        assertFalse(tracker.isActive());
        assertTrue(engine.personalBestMillis("slayer.inferno.3.toSpawn").isEmpty(), "grind split cancelled");
        assertEquals(40_000L, engine.personalBestMillis("slayer.inferno.3.total").orElseThrow());
    }

    @Test
    void newQuestWhileActiveResetsCleanly() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        SlayerTracker tracker = new SlayerTracker(engine);

        tracker.onQuestStart("revenant", 1, 0L);
        tracker.onBossSpawn("p", 5_000L);
        // Abruptly start a new quest (e.g. detector missed the end) — must not leak old timers.
        tracker.onQuestStart("tarantula", 2, 10_000L);
        tracker.onBossSpawn("p", 12_000L);
        tracker.onComplete("p", 15_000L);

        assertEquals(2_000L, engine.personalBestMillis("slayer.tarantula.2.toSpawn").orElseThrow());
        assertEquals(3_000L, engine.personalBestMillis("slayer.tarantula.2.fight").orElseThrow());
        assertTrue(engine.personalBestMillis("slayer.revenant.1.total").isEmpty());
    }
}
