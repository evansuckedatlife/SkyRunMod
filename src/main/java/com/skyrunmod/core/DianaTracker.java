package com.skyrunmod.core;

import com.skyrunmod.core.SkyRunAnalyticsEngine.Activity;

/**
 * Times Diana burrow chains: the gap between consecutive burrow digs ({@code diana.burrow.gap}, so
 * the PB is your fastest chain step / EMA your typical pace) and the time from the chain start to a
 * Minos Inquisitor dig ({@code diana.inquisitor}). Minecraft-free, unit tested.
 *
 * <p>A gap longer than {@link #CHAIN_RESET_MILLIS} is treated as a new chain rather than one slow
 * step, so walking away and starting fresh doesn't pollute the PB.
 */
public final class DianaTracker {
    /** Gaps longer than this start a fresh chain instead of counting as one step. */
    public static final long CHAIN_RESET_MILLIS = 60_000L;

    private final SkyRunAnalyticsEngine engine;

    private long chainStartMillis = -1;
    private long lastDigMillis = -1;

    public DianaTracker(SkyRunAnalyticsEngine engine) {
        this.engine = engine;
    }

    /** Records a burrow dig, timing the gap from the previous dig in the same chain. */
    public void onBurrowDig(String actor, long nowMillis) {
        engine.overlayModel().setActivity(Activity.DIANA);

        if (lastDigMillis < 0 || nowMillis - lastDigMillis > CHAIN_RESET_MILLIS) {
            // Start of a new chain.
            chainStartMillis = nowMillis;
        } else {
            long start = lastDigMillis;
            engine.startSplit("diana.burrow.gap", start);
            engine.completeSplit("diana.burrow.gap", actor, nowMillis);
            engine.overlayModel().setSection("diana", "Burrow +" + (nowMillis - start) + "ms", nowMillis);
        }
        lastDigMillis = nowMillis;
    }

    /** Records the time from the current chain's start to an inquisitor dig. */
    public void onInquisitor(String actor, long nowMillis) {
        if (chainStartMillis < 0) {
            chainStartMillis = nowMillis;
        }
        engine.startSplit("diana.inquisitor", chainStartMillis);
        engine.completeSplit("diana.inquisitor", actor, nowMillis);
        engine.overlayModel().setActivity(Activity.DIANA);
        engine.overlayModel().setSection("diana", "Inquisitor!", nowMillis);
        lastDigMillis = nowMillis;
    }

    public void reset() {
        chainStartMillis = -1;
        lastDigMillis = -1;
    }
}
