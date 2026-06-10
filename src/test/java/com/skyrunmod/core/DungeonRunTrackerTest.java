package com.skyrunmod.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DungeonRunTrackerTest {

    @Test
    void parsesFloorFromSidebar() {
        assertEquals("F7", DungeonRunParser.parseFloor(List.of("The Catacombs (F7)")).orElseThrow());
        assertEquals("M7", DungeonRunParser.parseFloor(List.of("The Catacombs (M7)")).orElseThrow());
        assertTrue(DungeonRunParser.parseFloor(List.of("Hub")).isEmpty());
    }

    @Test
    void recognizesMilestoneChat() {
        assertTrue(DungeonRunParser.isDungeonStart("Dungeon starts in 1 second."));
        assertTrue(DungeonRunParser.isBloodOpen("The BLOOD DOOR has been opened!"));
        assertTrue(DungeonRunParser.isBossEntry("[BOSS] Maxor: WELL! WELL! WELL!"));
        assertTrue(DungeonRunParser.isComplete("                    > EXTRA STATS <"));
        assertFalse(DungeonRunParser.isBossEntry("[NPC] Mort: Talk to me!"));
    }

    @Test
    void recordsCumulativeSplitsFromStart() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        DungeonRunTracker tracker = new DungeonRunTracker(engine);

        tracker.onStart("F7", 0L);
        tracker.onBlood("p", 60_000L);
        tracker.onBoss("p", 120_000L);
        tracker.onComplete("p", 200_000L);

        assertEquals(60_000L, engine.personalBestMillis("dungeon.floor.F7.blood").orElseThrow());
        assertEquals(120_000L, engine.personalBestMillis("dungeon.floor.F7.boss").orElseThrow());
        assertEquals(200_000L, engine.personalBestMillis("dungeon.floor.F7.clear").orElseThrow());
        assertFalse(tracker.isRunning());
    }

    @Test
    void ignoresDuplicateBossLines() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        DungeonRunTracker tracker = new DungeonRunTracker(engine);

        tracker.onStart("F7", 0L);
        tracker.onBoss("p", 100_000L);
        tracker.onBoss("p", 105_000L); // second [BOSS] line same run — must not overwrite
        assertEquals(100_000L, engine.personalBestMillis("dungeon.floor.F7.boss").orElseThrow());
        assertEquals(1, engine.recentSplits().size());
    }
}
