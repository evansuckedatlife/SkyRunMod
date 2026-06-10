package com.skyrunmod.core;

import com.skyrunmod.core.SkyRunAnalyticsEngine.Activity;

/**
 * Times a Kuudra run end to end, keyed by tier: {@code kuudra.t<tier>.total}. The fight start is
 * signalled by the Kuudra boss bar appearing; completion by the {@code KUUDRA DOWN} chat (with the
 * boss bar disappearing as a fallback). Phase-level splits (supply/build/dps) are a future addition.
 * Minecraft-free, unit tested.
 */
public final class KuudraTracker {
    private final SkyRunAnalyticsEngine engine;

    private boolean running;
    private int tier;
    private long startMillis;

    public KuudraTracker(SkyRunAnalyticsEngine engine) {
        this.engine = engine;
    }

    public void onStart(int tier, long nowMillis) {
        if (running) {
            return;
        }
        this.tier = tier;
        this.startMillis = nowMillis;
        this.running = true;
        engine.overlayModel().setActivity(Activity.KUUDRA);
        engine.overlayModel().setSection("kuudra", "Kuudra t" + tier + " started", nowMillis);
    }

    public void onComplete(String actor, long nowMillis) {
        if (!running) {
            return;
        }
        String key = "kuudra.t" + tier + ".total";
        engine.startSplit(key, startMillis);
        engine.completeSplit(key, actor, nowMillis);
        engine.overlayModel().setSection("kuudra", "Kuudra t" + tier + " down", nowMillis);
        running = false;
    }

    public void onAbort() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}
