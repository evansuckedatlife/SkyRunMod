package com.skyrunmod.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Turns a stream of commission-progress snapshots (from {@link CommissionParser}) into split timing
 * on a {@link SkyRunAnalyticsEngine}. Minecraft-free and unit tested.
 *
 * <p>Timing rules:
 * <ul>
 *   <li>A commission first seen at or below {@link #START_THRESHOLD} starts its timer, so personal
 *       bests measure (near) the full duration and stay comparable. One first seen already in
 *       progress (e.g. on login) is tracked for display but not timed.</li>
 *   <li>Reaching {@code DONE} (fraction {@code 1.0}) completes a timed commission once.</li>
 *   <li>A timed commission that vanishes from the tab while at/above
 *       {@link #NEAR_COMPLETE_ON_DISAPPEAR} is assumed completed (claimed before a {@code DONE} frame
 *       was polled); one that vanishes earlier is cancelled (the player left the area).</li>
 * </ul>
 */
public final class CommissionTracker {
    /** Max first-seen progress for which a commission is timed (10%). */
    public static final double START_THRESHOLD = 0.10d;
    /** A timed commission vanishing at/above this is treated as completed (90%). */
    public static final double NEAR_COMPLETE_ON_DISAPPEAR = 0.90d;

    private final SkyRunAnalyticsEngine engine;
    private final Map<String, Tracked> tracked = new HashMap<>();

    public CommissionTracker(SkyRunAnalyticsEngine engine) {
        this.engine = engine;
    }

    /**
     * Reconciles the latest tab snapshot against tracked state, emitting start/complete/cancel into
     * the engine.
     *
     * @param snapshot commission name → progress fraction in {@code [0,1]} ({@code 1.0} == DONE)
     * @param actor    the local player's name, attributed to completed splits
     * @param nowMillis monotonic timestamp
     */
    public void update(Map<String, Double> snapshot, String actor, long nowMillis) {
        for (Map.Entry<String, Double> entry : snapshot.entrySet()) {
            String name = entry.getKey();
            double fraction = entry.getValue();
            Tracked state = tracked.get(name);

            if (state == null) {
                boolean timed = fraction <= START_THRESHOLD;
                if (timed) {
                    engine.commissionStart(name, nowMillis);
                }
                state = new Tracked(timed);
                tracked.put(name, state);
            }
            state.lastFraction = fraction;

            if (fraction >= 1.0d && !state.completed) {
                if (state.timed) {
                    engine.commissionComplete(name, actor, nowMillis);
                }
                state.completed = true; // keep tracked until it disappears (claimed) to avoid re-firing
            }
        }

        // Reconcile commissions that disappeared from the tab this update.
        Iterator<Map.Entry<String, Tracked>> it = tracked.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Tracked> e = it.next();
            String name = e.getKey();
            Tracked state = e.getValue();
            if (snapshot.containsKey(name)) {
                continue;
            }
            if (state.timed && !state.completed) {
                if (state.lastFraction >= NEAR_COMPLETE_ON_DISAPPEAR) {
                    engine.commissionComplete(name, actor, nowMillis);
                } else {
                    engine.cancelSplit("commission." + name);
                }
            }
            it.remove();
        }
    }

    /**
     * Completes a commission from the authoritative chat broadcast (instant and frame-accurate),
     * matching {@code chatName} against tracked commissions case-insensitively (chat is upper-case,
     * the tab is title case). No-op if the commission isn't tracked (we never saw it start) or was
     * already completed via the tab. Returns true if a tracked commission was matched.
     */
    public boolean completeByName(String chatName, String actor, long nowMillis) {
        if (chatName == null) {
            return false;
        }
        for (Map.Entry<String, Tracked> e : tracked.entrySet()) {
            if (e.getKey().equalsIgnoreCase(chatName.trim())) {
                Tracked state = e.getValue();
                if (state.timed && !state.completed) {
                    engine.commissionComplete(e.getKey(), actor, nowMillis);
                }
                state.completed = true; // suppress the slower tab DONE path
                return true;
            }
        }
        return false;
    }

    /** Visible for tests: number of commissions currently being tracked. */
    public int trackedCount() {
        return tracked.size();
    }

    private static final class Tracked {
        final boolean timed;
        double lastFraction;
        boolean completed;

        Tracked(boolean timed) {
            this.timed = timed;
        }
    }
}
