package com.skyrunmod.client;

import com.skyrunmod.config.PersonalBestStore;
import com.skyrunmod.config.SkyRunConfig;
import com.skyrunmod.config.SkyRunPaths;
import com.skyrunmod.core.CommissionTracker;
import com.skyrunmod.core.DianaTracker;
import com.skyrunmod.core.DungeonRunTracker;
import com.skyrunmod.core.FishingTracker;
import com.skyrunmod.core.KuudraTracker;
import com.skyrunmod.core.MiningTracker;
import com.skyrunmod.core.SessionRates;
import com.skyrunmod.core.SkyRunAnalyticsEngine;
import com.skyrunmod.core.SlayerTracker;
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
    private final CommissionTracker commissionTracker;
    private final SlayerTracker slayerTracker;
    private final DungeonRunTracker dungeonTracker;
    private final KuudraTracker kuudraTracker;
    private final DianaTracker dianaTracker;
    private final MiningTracker miningTracker;
    private final FishingTracker fishingTracker;
    private final SessionRates sessionRates = new SessionRates();

    private SkyRunState(SkyRunAnalyticsEngine engine, SkyRunConfig config, PersonalBestStore store) {
        this.engine = engine;
        this.config = config;
        this.personalBestStore = store;
        this.commissionTracker = new CommissionTracker(engine);
        this.slayerTracker = new SlayerTracker(engine);
        this.dungeonTracker = new DungeonRunTracker(engine);
        this.kuudraTracker = new KuudraTracker(engine);
        this.dianaTracker = new DianaTracker(engine);
        this.miningTracker = new MiningTracker(engine);
        this.fishingTracker = new FishingTracker(engine);
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

    public CommissionTracker commissionTracker() {
        return commissionTracker;
    }

    public SlayerTracker slayerTracker() {
        return slayerTracker;
    }

    public DungeonRunTracker dungeonTracker() {
        return dungeonTracker;
    }

    public KuudraTracker kuudraTracker() {
        return kuudraTracker;
    }

    public DianaTracker dianaTracker() {
        return dianaTracker;
    }

    public MiningTracker miningTracker() {
        return miningTracker;
    }

    public FishingTracker fishingTracker() {
        return fishingTracker;
    }

    public SessionRates sessionRates() {
        return sessionRates;
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

    /** Clears in-flight run state + session rates, persisting PBs first so nothing earned is lost. */
    public void resetRun() {
        if (config.persistPersonalBests) {
            personalBestStore.save(engine);
        }
        engine.resetRun();
        sessionRates.reset();
    }
}
