package com.skyrunmod.core;

import com.skyrunmod.core.SkyRunAnalyticsEngine.Activity;

/**
 * Times a Catacombs run as cumulative-from-start splits (LiveSplit style):
 * <ul>
 *   <li>{@code dungeon.floor.<F>.blood} — start → blood door opened</li>
 *   <li>{@code dungeon.floor.<F>.boss}  — start → boss-room entry</li>
 *   <li>{@code dungeon.floor.<F>.clear} — start → run complete</li>
 * </ul>
 *
 * Each split is measured from a single run-start anchor (re-arming a fresh timer per milestone, the
 * same approach as Goldor terminals). Minecraft-free and unit tested.
 */
public final class DungeonRunTracker {
    private final SkyRunAnalyticsEngine engine;

    private boolean running;
    private String floor;
    private long startMillis;
    private boolean bloodDone;
    private boolean bossDone;

    public DungeonRunTracker(SkyRunAnalyticsEngine engine) {
        this.engine = engine;
    }

    public void onStart(String floor, long nowMillis) {
        this.floor = floor;
        this.startMillis = nowMillis;
        this.running = true;
        this.bloodDone = false;
        this.bossDone = false;
        engine.overlayModel().setActivity(Activity.DUNGEON_FLOOR);
        engine.overlayModel().setSection("dungeon", floor + " started", nowMillis);
    }

    public void onBlood(String actor, long nowMillis) {
        if (running && !bloodDone) {
            record("blood", actor, nowMillis);
            bloodDone = true;
        }
    }

    public void onBoss(String actor, long nowMillis) {
        if (running && !bossDone) {
            record("boss", actor, nowMillis);
            bossDone = true;
        }
    }

    public void onComplete(String actor, long nowMillis) {
        if (!running) {
            return;
        }
        record("clear", actor, nowMillis);
        running = false;
    }

    public void onAbort() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public String floor() {
        return floor;
    }

    private void record(String segment, String actor, long nowMillis) {
        String key = "dungeon.floor." + floor + "." + segment;
        engine.startSplit(key, startMillis);
        engine.completeSplit(key, actor, nowMillis);
        engine.overlayModel().setSection("dungeon", floor + " " + segment, nowMillis);
    }
}
