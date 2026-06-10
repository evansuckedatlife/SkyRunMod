package com.skyrunmod.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skyrunmod.core.SkyRunAnalyticsEngine;

/**
 * Loads and saves an engine's personal bests + EMAs as JSON. Kept separate from the engine so the
 * core stays free of Gson and is unit-testable on a bare JVM.
 */
public final class PersonalBestStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;

    public PersonalBestStore(Path file) {
        this.file = file;
    }

    /** Reads the JSON file (if present) into the engine. Missing or corrupt files are ignored. */
    public void loadInto(SkyRunAnalyticsEngine engine) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            PersistedState state = GSON.fromJson(reader, PersistedState.class);
            if (state != null) {
                engine.restore(
                        state.personalBests == null ? Map.of() : state.personalBests,
                        state.emas == null ? Map.of() : state.emas);
            }
        } catch (IOException | RuntimeException e) {
            // A bad PB file should never take the client down; start with empty analytics instead.
            System.err.println("[SkyRunMod] Failed to read personal bests from " + file + ": " + e.getMessage());
        }
    }

    /** Writes the engine's current PBs + EMAs to disk, creating parent directories as needed. */
    public void save(SkyRunAnalyticsEngine engine) {
        PersistedState state = new PersistedState();
        state.personalBests = new HashMap<>(engine.personalBests());
        state.emas = new HashMap<>(engine.emaSnapshot());
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(state, writer);
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("[SkyRunMod] Failed to write personal bests to " + file + ": " + e.getMessage());
        }
    }

    /** JSON shape on disk. Field names are the serialized keys. */
    private static final class PersistedState {
        Map<String, Long> personalBests;
        Map<String, Double> emas;
    }
}
