package com.skyrunmod.core;

import com.skyrunmod.core.SkyRunAnalyticsEngine.Activity;

/**
 * State machine for a single Slayer quest, producing three splits per quest:
 * <ul>
 *   <li>{@code slayer.<type>.<tier>.toSpawn} — quest start → boss spawn (the XP grind)</li>
 *   <li>{@code slayer.<type>.<tier>.fight}   — boss spawn → boss slain</li>
 *   <li>{@code slayer.<type>.<tier>.total}   — quest start → boss slain</li>
 * </ul>
 *
 * Minecraft-free and unit tested. {@link com.skyrunmod.client.SlayerDetector} calls the lifecycle
 * methods from scoreboard transitions (+ a precise chat completion).
 */
public final class SlayerTracker {
    private enum Phase { NONE, GRINDING, BOSS }

    private final SkyRunAnalyticsEngine engine;
    private Phase phase = Phase.NONE;
    private String type;
    private int tier;

    public SlayerTracker(SkyRunAnalyticsEngine engine) {
        this.engine = engine;
    }

    /** A new quest began. {@code type}/{@code tier} key the splits. */
    public void onQuestStart(String type, int tier, long nowMillis) {
        if (phase != Phase.NONE) {
            onAbort(); // defensive: never leak timers from a prior quest
        }
        this.type = type;
        this.tier = tier;
        this.phase = Phase.GRINDING;
        engine.startSplit(prefix() + ".toSpawn", nowMillis);
        engine.startSplit(prefix() + ".total", nowMillis);
        engine.overlayModel().setActivity(Activity.SLAYER);
        engine.overlayModel().setSection("slayer", label() + " — grinding", nowMillis);
    }

    /** The boss spawned; closes the grind split and opens the fight split. */
    public void onBossSpawn(String actor, long nowMillis) {
        if (phase != Phase.GRINDING) {
            return;
        }
        engine.completeSplit(prefix() + ".toSpawn", actor, nowMillis);
        engine.startSplit(prefix() + ".fight", nowMillis);
        phase = Phase.BOSS;
        engine.overlayModel().setSection("slayer", label() + " — boss up", nowMillis);
    }

    /** The boss was slain; closes the fight + total splits. */
    public void onComplete(String actor, long nowMillis) {
        if (phase == Phase.NONE) {
            return;
        }
        if (phase == Phase.GRINDING) {
            // Boss spawn was never observed; we can't time the fight, but still close the grind timer.
            engine.cancelSplit(prefix() + ".toSpawn");
        } else {
            engine.completeSplit(prefix() + ".fight", actor, nowMillis);
        }
        engine.completeSplit(prefix() + ".total", actor, nowMillis);
        engine.overlayModel().setSection("slayer", label() + " — slain", nowMillis);
        phase = Phase.NONE;
    }

    /** The quest ended without a kill (cancelled / left area). */
    public void onAbort() {
        if (phase == Phase.NONE) {
            return;
        }
        engine.cancelSplit(prefix() + ".toSpawn");
        engine.cancelSplit(prefix() + ".fight");
        engine.cancelSplit(prefix() + ".total");
        phase = Phase.NONE;
    }

    public boolean isActive() {
        return phase != Phase.NONE;
    }

    public boolean isBossUp() {
        return phase == Phase.BOSS;
    }

    private String prefix() {
        return "slayer." + type + "." + tier;
    }

    private String label() {
        return capitalize(type) + " " + tier;
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return String.valueOf(value);
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
