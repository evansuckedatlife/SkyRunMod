package com.skyrunmod.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CommissionParserTest {

    @Test
    void parsesPercentAndDonePreservingOrder() {
        List<String> tab = List.of(
                "Yourname",
                "Commissions",
                " Mithril Miner: 30.5%",
                " Goblin Slayer: DONE",
                "Profile: Apple",   // not a commission (no %/DONE)
                "Players (2)");

        Map<String, Double> result = CommissionParser.parse(tab);

        assertEquals(2, result.size());
        assertEquals(List.of("Mithril Miner", "Goblin Slayer"), new ArrayList<>(result.keySet()));
        assertEquals(0.305d, result.get("Mithril Miner"), 1e-9);
        assertEquals(1.0d, result.get("Goblin Slayer"), 1e-9);
    }

    @Test
    void requiresCommissionHeader() {
        List<String> noHeader = List.of("Yourname", "Mithril Miner: 30%", "Players (2)");
        assertFalse(CommissionParser.hasHeader(noHeader));
        assertTrue(CommissionParser.parse(noHeader).isEmpty());

        List<String> withHeader = List.of("Commissions", "Mithril Miner: 30%");
        assertTrue(CommissionParser.hasHeader(withHeader));
        assertEquals(1, CommissionParser.parse(withHeader).size());
    }

    @Test
    void headerWithNoActiveCommissionsIsEmptyButDetected() {
        List<String> tab = List.of("Commissions", "Players (1)");
        assertTrue(CommissionParser.hasHeader(tab));
        assertTrue(CommissionParser.parse(tab).isEmpty());
    }

    @Test
    void parsesRealDwarvenMinesTabFormat() {
        // Exact format confirmed in-game: "Commissions:" header (with colon) + "Name: 0%" lines.
        List<String> tab = List.of(
                "Commissions:",
                "Raffle: 0%",
                "Lava Springs Mithril: 0%",
                "Goblin Raid: 0%",
                "Elusive Goblin Slayer: 0%",
                "Players (5)");

        assertTrue(CommissionParser.hasHeader(tab));
        Map<String, Double> result = CommissionParser.parse(tab);

        assertEquals(4, result.size(), "the 'Commissions:' header must not be parsed as a commission");
        assertEquals(List.of("Raffle", "Lava Springs Mithril", "Goblin Raid", "Elusive Goblin Slayer"),
                new ArrayList<>(result.keySet()));
        result.values().forEach(frac -> assertEquals(0.0d, frac, 1e-9));
    }

    @Test
    void parsesCompletionBroadcast() {
        assertEquals("LAVA SPRINGS MITHRIL",
                CommissionParser.parseCompletionName(
                        "LAVA SPRINGS MITHRIL Commission Complete! Visit the King to claim your rewards!")
                        .orElseThrow());
        assertEquals("GOBLIN SLAYER",
                CommissionParser.parseCompletionName("GOBLIN SLAYER Commission Complete!").orElseThrow());
        assertTrue(CommissionParser.parseCompletionName("Some unrelated chat line").isEmpty());
    }

    @Test
    void handlesAreaNamedCommissionsAndCommaDecimals() {
        List<String> tab = List.of(
                "Commissions",
                "Cliffside Veins Mithril: 12,5%",
                "Lush Caves Titanium: 100%");

        Map<String, Double> result = CommissionParser.parse(tab);
        assertEquals(0.125d, result.get("Cliffside Veins Mithril"), 1e-9);
        assertEquals(1.0d, result.get("Lush Caves Titanium"), 1e-9);
    }
}
