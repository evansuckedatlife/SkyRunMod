package com.skyrunmod.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * User-tunable overlay settings, persisted as {@code config/skyrunmod/settings.json}. Plain public
 * fields keep the Gson round-trip trivial and make this usable as a backing bean for a future
 * cloth-config screen.
 */
public final class SkyRunConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Master toggle for drawing the overlay. */
    public boolean overlayEnabled = true;
    /** Top-left anchor of the overlay, in scaled GUI pixels. */
    public int overlayX = 4;
    public int overlayY = 4;
    /** Text scale multiplier (1.0 = vanilla font size). */
    public float overlayScale = 1.0f;
    /** Draw the translucent backdrop behind the overlay text. */
    public boolean overlayBackground = true;
    /** Show the per-split EMA ("recent average") column. */
    public boolean showEma = true;
    /** Show the signed delta versus the previous personal best. */
    public boolean showPbDelta = true;
    /** Max number of split rows rendered in the live list. */
    public int maxSplitRows = 8;
    /** Status lines older than this (ms) are pruned from the overlay. */
    public long staleSectionTtlMillis = 15_000L;
    /** Persist personal bests/EMAs to disk on shutdown and run reset. */
    public boolean persistPersonalBests = true;

    public static SkyRunConfig load(Path file) {
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                SkyRunConfig loaded = GSON.fromJson(reader, SkyRunConfig.class);
                if (loaded != null) {
                    return loaded.sanitized();
                }
            } catch (IOException | RuntimeException e) {
                System.err.println("[SkyRunMod] Failed to read settings from " + file + ": " + e.getMessage());
            }
        }
        SkyRunConfig fresh = new SkyRunConfig();
        fresh.save(file);
        return fresh;
    }

    public void save(Path file) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("[SkyRunMod] Failed to write settings to " + file + ": " + e.getMessage());
        }
    }

    /** Clamps deserialized values into sane ranges so a hand-edited file can't break rendering. */
    private SkyRunConfig sanitized() {
        overlayScale = clamp(overlayScale, 0.5f, 3.0f);
        overlayX = Math.max(0, overlayX);
        overlayY = Math.max(0, overlayY);
        maxSplitRows = (int) clamp(maxSplitRows, 1, SkyRunConfigLimits.MAX_ROWS);
        staleSectionTtlMillis = Math.max(1_000L, staleSectionTtlMillis);
        return this;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class SkyRunConfigLimits {
        static final int MAX_ROWS = 32;
    }
}
