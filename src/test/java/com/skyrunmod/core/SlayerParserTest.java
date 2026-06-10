package com.skyrunmod.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class SlayerParserTest {

    @Test
    void parsesTypeAndTierWhileGrinding() {
        List<String> sidebar = List.of(
                "SKYBLOCK",
                "Slayer Quest",
                "Revenant Horror V",
                "Combat XP needed: 312");
        SlayerParser.Snapshot snap = SlayerParser.parseSnapshot(sidebar).orElseThrow();
        assertEquals("revenant", snap.type());
        assertEquals(5, snap.tier());
        assertFalse(snap.bossActive());
    }

    @Test
    void detectsBossActive() {
        List<String> sidebar = List.of(
                "Slayer Quest",
                "Voidgloom Seraph III",
                "Slay the boss!");
        SlayerParser.Snapshot snap = SlayerParser.parseSnapshot(sidebar).orElseThrow();
        assertEquals("voidgloom", snap.type());
        assertEquals(3, snap.tier());
        assertTrue(snap.bossActive());
    }

    @Test
    void emptyWhenNoSlayerOnBoard() {
        assertTrue(SlayerParser.parseSnapshot(List.of("SKYBLOCK", "Purse: 1,000")).isEmpty());
    }

    @Test
    void recognizesChatEvents() {
        assertTrue(SlayerParser.isQuestStarted("  SLAYER QUEST STARTED!"));
        assertTrue(SlayerParser.isQuestComplete("  SLAYER QUEST COMPLETE!"));
        assertTrue(SlayerParser.isQuestComplete("  NICE! SLAYER BOSS SLAIN!"));
        assertFalse(SlayerParser.isQuestComplete("Some other message"));
    }
}
