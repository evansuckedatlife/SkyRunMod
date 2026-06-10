package com.skyrunmod.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import com.skyrunmod.config.SkyRunConfig;
import com.skyrunmod.core.SkyRunAnalyticsEngine;
import com.skyrunmod.core.SkyRunAnalyticsEngine.Activity;
import com.skyrunmod.core.SkyRunAnalyticsEngine.SplitRecord;
import com.skyrunmod.util.TimeFormat;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

/**
 * LiveSplit-style HUD overlay. Renders the current activity, any live status lines, and a table of
 * recent splits with their time, signed delta versus the previous personal best (green ahead / red
 * behind / gold for a first-ever best) and an optional recency-weighted average.
 */
public final class SplitOverlay implements HudElement {
    private static final int COLUMN_GAP = 6;

    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_ACCENT = 0xFF55FFFF;
    private static final int COLOR_SECTION = 0xFFAAAAAA;
    private static final int COLOR_LABEL = 0xFFBBBBBB;
    private static final int COLOR_TIME = 0xFFFFFFFF;
    private static final int COLOR_AHEAD = 0xFF55FF55;
    private static final int COLOR_BEHIND = 0xFFFF5555;
    private static final int COLOR_GOLD = 0xFFFFAA00;
    private static final int COLOR_EMA = 0xFF888888;
    private static final int COLOR_BACKGROUND = 0x90000000;

    public static void register() {
        HudElementRegistry.addLast(Identifier.of("skyrunmod", "split_overlay"), new SplitOverlay());
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        SkyRunState state = SkyRunState.get();
        if (state == null) {
            return;
        }
        SkyRunConfig config = state.config();
        if (!config.overlayEnabled) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.options.hudHidden) {
            return;
        }

        SkyRunAnalyticsEngine engine = state.engine();
        engine.overlayModel().pruneStale(state.now(), config.staleSectionTtlMillis);

        List<Line> lines = buildLines(client.textRenderer, engine, config);
        if (lines.isEmpty()) {
            return;
        }

        int lineHeight = client.textRenderer.fontHeight + 2;
        int width = 0;
        for (Line line : lines) {
            width = Math.max(width, line.width(client.textRenderer));
        }
        int height = lines.size() * lineHeight;

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(config.overlayX, config.overlayY);
        matrices.scale(config.overlayScale);

        if (config.overlayBackground) {
            context.fill(-3, -3, width + 3, height + 1, COLOR_BACKGROUND);
        }

        int y = 0;
        for (Line line : lines) {
            int x = 0;
            for (Segment segment : line.segments) {
                context.drawText(client.textRenderer, segment.text, x, y, segment.color, true);
                x += client.textRenderer.getWidth(segment.text) + segment.trailingGap;
            }
            y += lineHeight;
        }

        matrices.popMatrix();
    }

    private static List<Line> buildLines(TextRenderer textRenderer, SkyRunAnalyticsEngine engine, SkyRunConfig config) {
        List<Line> lines = new ArrayList<>();
        SkyRunAnalyticsEngine.OverlayModel overlay = engine.overlayModel();

        Line title = new Line();
        title.add("SkyRun", COLOR_ACCENT, COLUMN_GAP);
        title.add(prettyActivity(overlay.activity()), COLOR_TITLE, 0);
        lines.add(title);

        for (Map.Entry<String, String> section : overlay.sections().entrySet()) {
            Line line = new Line();
            line.add(section.getValue(), COLOR_SECTION, 0);
            lines.add(line);
        }

        List<SplitRecord> recent = engine.recentSplits();
        int from = Math.max(0, recent.size() - config.maxSplitRows);
        for (int i = from; i < recent.size(); i++) {
            lines.add(splitLine(recent.get(i), engine, config));
        }
        return lines;
    }

    private static Line splitLine(SplitRecord record, SkyRunAnalyticsEngine engine, SkyRunConfig config) {
        Line line = new Line();
        line.add(label(record.key()), COLOR_LABEL, COLUMN_GAP);
        line.add(TimeFormat.duration(record.elapsedMillis()), COLOR_TIME, COLUMN_GAP);

        if (config.showPbDelta) {
            OptionalLong delta = record.deltaToPreviousBest();
            if (delta.isEmpty()) {
                line.add(record.personalBest() ? "PB" : "—", record.personalBest() ? COLOR_GOLD : COLOR_LABEL, COLUMN_GAP);
            } else {
                long d = delta.getAsLong();
                int color = d <= 0 ? COLOR_AHEAD : COLOR_BEHIND;
                line.add(TimeFormat.signedDelta(d), color, COLUMN_GAP);
            }
        }

        if (config.showEma) {
            line.add("~" + TimeFormat.duration((long) record.emaMillis()), COLOR_EMA, 0);
        }
        return line;
    }

    /** Turns an internal split key into a short, human label for the overlay. */
    static String label(String key) {
        if (key.startsWith("dungeon.maxor.crystal_pickup")) {
            return "Crystal";
        }
        if (key.startsWith("dungeon.maxor.crystal_place")) {
            return "Crystal placed";
        }
        if (key.startsWith("dungeon.goldor.")) {
            String[] parts = key.split("\\.");
            // dungeon.goldor.<type>.<index>
            if (parts.length >= 4) {
                return capitalize(parts[2]) + " " + parts[3];
            }
            return "Goldor";
        }
        if (key.startsWith("dungeon.m7.dragon.")) {
            String[] parts = key.split("\\.");
            if (parts.length >= 4) {
                return capitalize(parts[3]) + " dragon";
            }
            return "Dragon";
        }
        if (key.startsWith("dungeon.m7.relic")) {
            return "Relic";
        }
        // Fallback: last dotted segment.
        int dot = key.lastIndexOf('.');
        return dot >= 0 ? key.substring(dot + 1) : key;
    }

    private static String prettyActivity(Activity activity) {
        return switch (activity) {
            case IDLE -> "Idle";
            case DUNGEON_MAXOR -> "F7 · Maxor";
            case DUNGEON_STORM -> "F7 · Storm";
            case DUNGEON_GOLDOR -> "F7 · Goldor";
            case DUNGEON_M7_DRAGON -> "M7 · Dragons";
            case ROOM_CLEAR -> "Room Clear";
            case ROUTE_EXECUTION -> "Route";
            case COMMISSION -> "Commission";
            case TRANSITION -> "Transition";
        };
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    /** One rendered row, a sequence of colored text segments. */
    private static final class Line {
        private final List<Segment> segments = new ArrayList<>();

        void add(String text, int color, int trailingGap) {
            segments.add(new Segment(text, color, trailingGap));
        }

        int width(TextRenderer textRenderer) {
            int w = 0;
            for (Segment segment : segments) {
                w += textRenderer.getWidth(segment.text) + segment.trailingGap;
            }
            return w;
        }
    }

    private record Segment(String text, int color, int trailingGap) {
    }
}
