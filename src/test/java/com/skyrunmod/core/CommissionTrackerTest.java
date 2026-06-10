package com.skyrunmod.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import com.skyrunmod.core.SkyRunAnalyticsEngine.SplitRecord;

import org.junit.jupiter.api.Test;

class CommissionTrackerTest {

    private static Map<String, Double> snap(String name, double fraction) {
        return Map.of(name, fraction);
    }

    @Test
    void timesFromStartToDone() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        CommissionTracker tracker = new CommissionTracker(engine);

        tracker.update(snap("Mithril Miner", 0.0d), "p", 1000L);
        assertTrue(engine.isTracking("commission.Mithril Miner"));
        assertTrue(engine.recentSplits().isEmpty());

        tracker.update(snap("Mithril Miner", 0.5d), "p", 2000L);
        assertTrue(engine.recentSplits().isEmpty());

        tracker.update(snap("Mithril Miner", 1.0d), "p", 5000L);
        assertEquals(1, engine.recentSplits().size());
        SplitRecord split = engine.recentSplits().get(0);
        assertEquals("commission.Mithril Miner", split.key());
        assertEquals(4000L, split.elapsedMillis());
        assertTrue(split.personalBest());

        // Completed-but-listed (awaiting claim) is retained, then dropped when it disappears,
        // without re-firing the split.
        assertEquals(1, tracker.trackedCount());
        tracker.update(Map.of(), "p", 6000L);
        assertEquals(0, tracker.trackedCount());
        assertEquals(1, engine.recentSplits().size());
    }

    @Test
    void doesNotTimeCommissionFirstSeenMidway() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        CommissionTracker tracker = new CommissionTracker(engine);

        tracker.update(snap("Goblin Slayer", 0.5d), "p", 1000L);
        assertFalse(engine.isTracking("commission.Goblin Slayer"));

        tracker.update(snap("Goblin Slayer", 1.0d), "p", 2000L);
        assertTrue(engine.recentSplits().isEmpty(), "midway-seen commission yields no PB split");
    }

    @Test
    void disappearingNearCompleteCounts() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        CommissionTracker tracker = new CommissionTracker(engine);

        tracker.update(snap("X", 0.0d), "p", 0L);
        tracker.update(snap("X", 0.95d), "p", 1000L);
        tracker.update(Map.of(), "p", 2000L);

        assertEquals(1, engine.recentSplits().size());
        assertEquals(2000L, engine.recentSplits().get(0).elapsedMillis());
    }

    @Test
    void disappearingEarlyCancels() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        CommissionTracker tracker = new CommissionTracker(engine);

        tracker.update(snap("Y", 0.0d), "p", 0L);
        tracker.update(snap("Y", 0.4d), "p", 1000L);
        tracker.update(Map.of(), "p", 2000L);

        assertFalse(engine.isTracking("commission.Y"));
        assertTrue(engine.recentSplits().isEmpty());
        assertEquals(0, tracker.trackedCount());
    }

    @Test
    void chatCompletionMatchesTitleCaseTabNameAndSuppressesTabPath() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        CommissionTracker tracker = new CommissionTracker(engine);

        // Tab shows title case; the completion broadcast is upper case.
        tracker.update(snap("Lava Springs Mithril", 0.0d), "p", 1000L);
        assertTrue(tracker.completeByName("LAVA SPRINGS MITHRIL", "p", 4000L));

        assertEquals(1, engine.recentSplits().size());
        SplitRecord split = engine.recentSplits().get(0);
        assertEquals("commission.Lava Springs Mithril", split.key());
        assertEquals(3000L, split.elapsedMillis());

        // A later tab DONE frame must NOT record a second split.
        tracker.update(snap("Lava Springs Mithril", 1.0d), "p", 4500L);
        assertEquals(1, engine.recentSplits().size());
    }

    @Test
    void chatCompletionForUntrackedCommissionIsIgnored() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        CommissionTracker tracker = new CommissionTracker(engine);
        assertFalse(tracker.completeByName("NEVER SEEN", "p", 1000L));
        assertTrue(engine.recentSplits().isEmpty());
    }

    @Test
    void reappearingCommissionTimesFreshInstance() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        CommissionTracker tracker = new CommissionTracker(engine);

        tracker.update(snap("Z", 0.0d), "p", 0L);
        tracker.update(snap("Z", 1.0d), "p", 1000L);
        tracker.update(Map.of(), "p", 1100L); // claimed, leaves tab

        tracker.update(snap("Z", 0.0d), "p", 2000L);
        tracker.update(snap("Z", 1.0d), "p", 2500L);

        assertEquals(2, engine.recentSplits().size());
        assertEquals(1000L, engine.recentSplits().get(0).elapsedMillis());
        assertEquals(500L, engine.recentSplits().get(1).elapsedMillis());
    }
}
