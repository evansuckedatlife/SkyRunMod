package com.skyrunmod.core;

import java.util.HashSet;
import java.util.Set;

import com.skyrunmod.core.SkyRunAnalyticsEngine.Activity;

/**
 * Times mining runs:
 * <ul>
 *   <li>Crystal Hollows nucleus: from the first crystal, each new crystal records
 *       {@code mining.nucleus.<crystal>} (cumulative) and the fifth records {@code mining.nucleus.all5}.</li>
 *   <li>Mineshaft: the gap between discoveries ({@code mining.mineshaft.gap}).</li>
 * </ul>
 * Minecraft-free, unit tested.
 */
public final class MiningTracker {
    private final SkyRunAnalyticsEngine engine;

    private final Set<String> crystalsThisRun = new HashSet<>();
    private long nucleusStartMillis = -1;
    private long lastMineshaftMillis = -1;

    public MiningTracker(SkyRunAnalyticsEngine engine) {
        this.engine = engine;
    }

    /** A nucleus crystal was obtained. The first anchors the run; the fifth completes it. */
    public void onCrystal(String crystal, String actor, long nowMillis) {
        if (crystalsThisRun.contains(crystal)) {
            return; // already have it this run
        }
        engine.overlayModel().setActivity(Activity.MINING_RUN);

        if (crystalsThisRun.isEmpty()) {
            nucleusStartMillis = nowMillis; // anchor on first crystal
        } else {
            record("mining.nucleus." + crystal, actor, nowMillis);
        }
        crystalsThisRun.add(crystal);
        engine.overlayModel().setSection("mining", "Nucleus " + crystalsThisRun.size() + "/5", nowMillis);

        if (crystalsThisRun.size() >= 5) {
            record("mining.nucleus.all5", actor, nowMillis);
            crystalsThisRun.clear();
            nucleusStartMillis = -1;
        }
    }

    /** A Mineshaft was discovered; times the gap from the previous one. */
    public void onMineshaft(String actor, long nowMillis) {
        engine.overlayModel().setActivity(Activity.MINING_RUN);
        if (lastMineshaftMillis >= 0) {
            engine.startSplit("mining.mineshaft.gap", lastMineshaftMillis);
            engine.completeSplit("mining.mineshaft.gap", actor, nowMillis);
            engine.overlayModel().setSection("mining", "Mineshaft +" + (nowMillis - lastMineshaftMillis) + "ms", nowMillis);
        } else {
            engine.overlayModel().setSection("mining", "Mineshaft found", nowMillis);
        }
        lastMineshaftMillis = nowMillis;
    }

    public void reset() {
        crystalsThisRun.clear();
        nucleusStartMillis = -1;
        lastMineshaftMillis = -1;
    }

    private void record(String key, String actor, long nowMillis) {
        engine.startSplit(key, nucleusStartMillis);
        engine.completeSplit(key, actor, nowMillis);
    }
}
