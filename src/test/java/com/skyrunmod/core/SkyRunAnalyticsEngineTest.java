package com.skyrunmod.core;

public final class SkyRunAnalyticsEngineTest {
    public static void main(String[] args) {
        testCrystalAndTerminalParsing();
        testRoomPbAndEma();
        testOverlayClearsOnActivityChange();
        System.out.println("SkyRunAnalyticsEngineTest passed");
    }

    private static void testCrystalAndTerminalParsing() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine(0.5d);
        engine.startSplit("dungeon.maxor.crystal_pickup", 1000L);
        engine.onChatMessage("Alice picked up an energy crystal!", 1500L);

        assert engine.recentSplits().size() == 1;
        SkyRunAnalyticsEngine.SplitRecord crystal = engine.recentSplits().get(0);
        assert crystal.key().equals("dungeon.maxor.crystal_pickup");
        assert crystal.elapsedMillis() == 500L;
        assert crystal.personalBest();

        engine.startGoldor(2000L);
        engine.onChatMessage("Bob completed a terminal (1/7)!", 2400L);
        assert engine.recentSplits().size() == 2;
        SkyRunAnalyticsEngine.SplitRecord terminal = engine.recentSplits().get(1);
        assert terminal.key().equals("dungeon.goldor.terminal.1/7");
        assert terminal.actor().equals("Bob");
    }

    private static void testRoomPbAndEma() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine(0.5d);
        engine.roomEnter("LRoom", 0L);
        engine.roomExit("LRoom", "Alice", 1000L);

        engine.roomEnter("LRoom", 2000L);
        engine.roomExit("LRoom", "Alice", 2600L);

        assert engine.personalBestMillis("room.LRoom").orElseThrow() == 600L;
        double ema = engine.emaMillis("room.LRoom").orElseThrow();
        assert Math.abs(ema - 800.0d) < 0.0001d;
    }

    private static void testOverlayClearsOnActivityChange() {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        engine.startStorm(100L);
        assert engine.overlayModel().sections().containsKey("storm");

        engine.transitionStart("skyblock_load", 200L);
        assert engine.overlayModel().sections().containsKey("transition");
        assert !engine.overlayModel().sections().containsKey("storm");
    }
}
