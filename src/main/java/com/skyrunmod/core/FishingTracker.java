package com.skyrunmod.core;

import com.skyrunmod.core.SkyRunAnalyticsEngine.Activity;

/**
 * Times the gap between consecutive sea-creature spawns ({@code fishing.seacreature.gap} — PB =
 * fastest back-to-back, EMA = typical pace) and counts catches this session. Minecraft-free, unit
 * tested. Gaps beyond {@link #SESSION_RESET_MILLIS} start a fresh streak (you stopped fishing).
 */
public final class FishingTracker {
    public static final long SESSION_RESET_MILLIS = 5 * 60_000L;

    private final SkyRunAnalyticsEngine engine;

    private long lastSpawnMillis = -1;
    private int catches;

    public FishingTracker(SkyRunAnalyticsEngine engine) {
        this.engine = engine;
    }

    public void onSeaCreature(String creature, String actor, long nowMillis) {
        engine.overlayModel().setActivity(Activity.FISHING);
        catches++;

        if (lastSpawnMillis >= 0 && nowMillis - lastSpawnMillis <= SESSION_RESET_MILLIS) {
            engine.startSplit("fishing.seacreature.gap", lastSpawnMillis);
            engine.completeSplit("fishing.seacreature.gap", actor, nowMillis);
        }
        lastSpawnMillis = nowMillis;
        engine.overlayModel().setSection("fishing", "Catch #" + catches + " (" + creature + ")", nowMillis);
    }

    public int catches() {
        return catches;
    }

    public void reset() {
        lastSpawnMillis = -1;
        catches = 0;
    }
}
