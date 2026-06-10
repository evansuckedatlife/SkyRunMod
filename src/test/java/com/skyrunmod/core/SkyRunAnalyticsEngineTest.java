package com.skyrunmod.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.OptionalLong;

import com.skyrunmod.core.SkyRunAnalyticsEngine.SplitRecord;

import org.junit.jupiter.api.Test;

class SkyRunAnalyticsEngineTest {

    @Test
    void crystalPickupCompletesArmedSplit() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine(0.5d);
        engine.startSplit("dungeon.maxor.crystal_pickup", 1000L);
        engine.onChatMessage("Alice picked up an energy crystal!", 1500L);

        assertEquals(1, engine.recentSplits().size());
        SplitRecord crystal = engine.recentSplits().get(0);
        assertEquals("dungeon.maxor.crystal_pickup", crystal.key());
        assertEquals(500L, crystal.elapsedMillis());
        assertEquals("Alice", crystal.actor());
        assertTrue(crystal.personalBest());
        assertTrue(crystal.deltaToPreviousBest().isEmpty(), "first ever split has no previous best");
    }

    @Test
    void goldorObjectivesParseActivatedAndCompletedForAllTypes() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        engine.startGoldor(2000L);

        engine.onChatMessage("Bob activated a terminal (1/7)!", 2400L);
        engine.onChatMessage("Cara completed a device (1/2)!", 2600L);
        engine.onChatMessage("Dan activated a lever!", 2800L);

        assertEquals(3, engine.recentSplits().size());
        assertEquals("dungeon.goldor.terminal.1/7", engine.recentSplits().get(0).key());
        assertEquals("dungeon.goldor.device.1/2", engine.recentSplits().get(1).key());
        assertEquals("dungeon.goldor.lever.?", engine.recentSplits().get(2).key());
        // Each objective is measured from Goldor phase start (2000L), not from the previous objective.
        assertEquals(400L, engine.recentSplits().get(0).elapsedMillis());
        assertEquals(800L, engine.recentSplits().get(2).elapsedMillis());
    }

    @Test
    void goldorObjectiveOutsidePhaseIsIgnored() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        assertTrue(engine.recordGoldorTerminal("Bob", "1/7", 1000L).isEmpty());
        engine.onChatMessage("Bob activated a terminal (1/7)!", 1200L);
        assertTrue(engine.recentSplits().isEmpty());
    }

    @Test
    void dragonKillAndRelicParse() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        engine.startSplit("dungeon.m7.dragon.red.kill", 0L);
        engine.startSplit("dungeon.m7.relic_pickup", 0L);

        engine.onChatMessage("Alice killed Red dragon!", 1000L);
        engine.onChatMessage("Bob picked up a relic!", 1500L);

        assertEquals("dungeon.m7.dragon.red.kill", engine.recentSplits().get(0).key());
        assertEquals("dungeon.m7.relic_pickup", engine.recentSplits().get(1).key());
    }

    @Test
    void roomPbAndEmaTrackAcrossRuns() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine(0.5d);
        engine.roomEnter("LRoom", 0L);
        engine.roomExit("LRoom", "Alice", 1000L);

        engine.roomEnter("LRoom", 2000L);
        engine.roomExit("LRoom", "Alice", 2600L);

        assertEquals(600L, engine.personalBestMillis("room.LRoom").orElseThrow());
        double ema = engine.emaMillis("room.LRoom").orElseThrow();
        assertEquals(800.0d, ema, 1e-9);
    }

    @Test
    void splitRecordsSignedDeltaAgainstPreviousBest() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        engine.roomEnter("X", 0L);
        engine.roomExit("X", "a", 1000L); // PB = 1000

        engine.roomEnter("X", 5000L);
        SplitRecord faster = engine.roomExit("X", "a", 5700L).orElseThrow(); // 700, new PB
        OptionalLong delta = faster.deltaToPreviousBest();
        assertTrue(delta.isPresent());
        assertEquals(-300L, delta.getAsLong());
        assertTrue(faster.personalBest());

        engine.roomEnter("X", 9000L);
        SplitRecord slower = engine.roomExit("X", "a", 9900L).orElseThrow(); // 900 vs PB 700
        assertEquals(200L, slower.deltaToPreviousBest().getAsLong());
        assertFalse(slower.personalBest());
    }

    @Test
    void overlayClearsSectionsOnActivityChange() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        engine.startStorm(100L);
        assertTrue(engine.overlayModel().sections().containsKey("storm"));

        engine.transitionStart("skyblock_load", 200L);
        assertTrue(engine.overlayModel().sections().containsKey("transition"));
        assertFalse(engine.overlayModel().sections().containsKey("storm"));
    }

    @Test
    void overlayPrunesStaleSectionsByAge() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        engine.startStorm(1000L);
        engine.overlayModel().pruneStale(2000L, 5000L);
        assertTrue(engine.overlayModel().sections().containsKey("storm"), "young section retained");
        engine.overlayModel().pruneStale(10_000L, 5000L);
        assertFalse(engine.overlayModel().sections().containsKey("storm"), "old section pruned");
    }

    @Test
    void recentSplitsAreBounded() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        int total = SkyRunAnalyticsEngine.MAX_RECENT_SPLITS + 50;
        for (int i = 0; i < total; i++) {
            engine.startSplit("k" + i, i * 10L);
            engine.completeSplit("k" + i, "a", i * 10L + 5L);
        }
        assertEquals(SkyRunAnalyticsEngine.MAX_RECENT_SPLITS, engine.recentSplits().size());
        assertEquals("k50", engine.recentSplits().get(0).key(), "oldest entries evicted FIFO");
    }

    @Test
    void sumOfBestAddsPersonalBestsUnderPrefix() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        engine.startSplit("dungeon.goldor.terminal.1/7", 0L);
        engine.completeSplit("dungeon.goldor.terminal.1/7", "a", 300L);
        engine.startSplit("dungeon.goldor.terminal.2/7", 0L);
        engine.completeSplit("dungeon.goldor.terminal.2/7", "a", 500L);
        engine.startSplit("room.Other", 0L);
        engine.completeSplit("room.Other", "a", 999L);

        assertEquals(800L, engine.sumOfBest("dungeon.goldor.terminal.").orElseThrow());
        assertTrue(engine.sumOfBest("nonexistent.").isEmpty());
    }

    @Test
    void restoreLoadsPersistedAnalytics() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        engine.restore(Map.of("room.A", 1234L), Map.of("room.A", 1300.0d));
        assertEquals(1234L, engine.personalBestMillis("room.A").orElseThrow());
        assertEquals(1300.0d, engine.emaMillis("room.A").orElseThrow(), 1e-9);
        // A later, slower run must not overwrite a better restored PB.
        engine.startSplit("room.A", 0L);
        SplitRecord slower = engine.completeSplit("room.A", "a", 2000L).orElseThrow();
        assertFalse(slower.personalBest());
        assertEquals(1234L, engine.personalBestMillis("room.A").orElseThrow());
    }

    @Test
    void resetRunClearsTimersAndHistoryButKeepsBests() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        engine.roomEnter("A", 0L);
        engine.roomExit("A", "a", 1000L);
        engine.roomEnter("B", 2000L); // timer left running

        engine.resetRun();

        assertTrue(engine.recentSplits().isEmpty());
        assertFalse(engine.isTracking("room.B"));
        assertEquals(SkyRunAnalyticsEngine.Activity.IDLE, engine.overlayModel().activity());
        assertEquals(1000L, engine.personalBestMillis("room.A").orElseThrow(), "PBs survive reset");
    }

    @Test
    void completeSplitRejectsNegativeDuration() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        engine.startSplit("transition.skyblock_load", 1000L);
        assertThrows(IllegalArgumentException.class,
                () -> engine.completeSplit("transition.skyblock_load", "Alice", 900L));
    }

    @Test
    void invalidEmaAlphaRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SkyRunAnalyticsEngine(0d));
        assertThrows(IllegalArgumentException.class, () -> new SkyRunAnalyticsEngine(1.5d));
    }
}
