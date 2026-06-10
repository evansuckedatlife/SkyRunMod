package com.skyrunmod.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class KuudraTrackerTest {

    @Test
    void parsesTierAndSignals() {
        assertEquals(4, KuudraParser.parseTier(List.of("Kuudra's Hollow", "Tier IV")));
        assertEquals(0, KuudraParser.parseTier(List.of("no tier here")));
        assertTrue(KuudraParser.isKuudraBossBar("Kuudra"));
        assertTrue(KuudraParser.isComplete("KUUDRA DOWN!"));
        assertFalse(KuudraParser.isComplete("Kuudra is angry"));
    }

    @Test
    void timesWholeRunByTier() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        KuudraTracker tracker = new KuudraTracker(engine);

        tracker.onStart(5, 0L);
        tracker.onStart(5, 1_000L); // duplicate boss-bar frame — ignored
        tracker.onComplete("p", 90_000L);

        assertEquals(90_000L, engine.personalBestMillis("kuudra.t5.total").orElseThrow());
        assertFalse(tracker.isRunning());
    }

    @Test
    void abortLeavesNoSplit() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        KuudraTracker tracker = new KuudraTracker(engine);
        tracker.onStart(3, 0L);
        tracker.onAbort();
        assertTrue(engine.recentSplits().isEmpty());
    }
}
