package com.skyrunmod.client;

import com.skyrunmod.config.PersonalBestStore;
import com.skyrunmod.config.SkyRunConfig;
import com.skyrunmod.config.SkyRunPaths;
import com.skyrunmod.core.SkyRunAnalyticsEngine;
import com.skyrunmod.util.MonotonicClock;

/**
 * Process-wide holder tying together the analytics engine, user settings and on-disk persistence.
 * Built once during client init via {@link #bootstrap()} and read by the overlay, chat listener,
 * keybinds and commands.
 */
public final class SkyRunState {
    private static volatile SkyRunState instance;

    private final SkyRunAnalyticsEngine engine;
    private final SkyRunConfig config;
    private final PersonalBestStore personalBestStore;

    private SkyRunState(SkyRunAnalyticsEngine engine, SkyRunConfig config, PersonalBestStore store) {
        this.engine = engine;
        this.config = config;
        this.personalBestStore = store;
    }

    /** Loads settings + saved PBs from disk and installs the singleton. Idempotent. */
    public static synchronized void bootstrap() {
        if (instance != null) {
            return;
        }
        SkyRunConfig config = SkyRunConfig.load(SkyRunPaths.settings());
        PersonalBestStore store = new PersonalBestStore(SkyRunPaths.personalBests());
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        store.loadInto(engine);
        instance = new SkyRunState(engine, config, store);
    }

    public static SkyRunState get() {
        return instance;
    }

    public SkyRunAnalyticsEngine engine() {
        return engine;
    }

    public SkyRunConfig config() {
        return config;
    }

    /** Monotonic millisecond timestamp for feeding the engine. */
    public long now() {
        return MonotonicClock.millis();
    }

    /** Persists settings, and PBs if persistence is enabled. Safe to call repeatedly. */
    public void persist() {
        config.save(SkyRunPaths.settings());
        if (config.persistPersonalBests) {
            personalBestStore.save(engine);
        }
    }

    /** Clears in-flight run state, persisting PBs first so nothing earned is lost. */
    public void resetRun() {
        if (config.persistPersonalBests) {
            personalBestStore.save(engine);
        }
        engine.resetRun();
    }
}
