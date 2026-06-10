package com.skyrunmod.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.skyrunmod.core.SkyRunAnalyticsEngine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PersonalBestStoreTest {

    @Test
    void savesAndReloadsPersonalBestsAndEmas(@TempDir Path dir) {
        Path file = dir.resolve("personal_bests.json");

        SkyRunAnalyticsEngine source = new SkyRunAnalyticsEngine(0.5d);
        source.startSplit("room.A", 0L);
        source.completeSplit("room.A", "p", 1000L);
        source.startSplit("room.A", 0L);
        source.completeSplit("room.A", "p", 600L); // PB now 600, EMA 800

        PersonalBestStore store = new PersonalBestStore(file);
        store.save(source);
        assertTrue(Files.isRegularFile(file));

        SkyRunAnalyticsEngine restored = new SkyRunAnalyticsEngine();
        new PersonalBestStore(file).loadInto(restored);

        assertEquals(600L, restored.personalBestMillis("room.A").orElseThrow());
        assertEquals(800.0d, restored.emaMillis("room.A").orElseThrow(), 1e-9);
    }

    @Test
    void missingFileLoadsNothingWithoutError(@TempDir Path dir) {
        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        new PersonalBestStore(dir.resolve("does_not_exist.json")).loadInto(engine);
        assertTrue(engine.personalBests().isEmpty());
    }

    @Test
    void corruptFileIsIgnored(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("personal_bests.json");
        Files.writeString(file, "{ this is not valid json ");

        SkyRunAnalyticsEngine engine = new SkyRunAnalyticsEngine();
        new PersonalBestStore(file).loadInto(engine); // must not throw
        assertTrue(engine.personalBests().isEmpty());
    }
}
