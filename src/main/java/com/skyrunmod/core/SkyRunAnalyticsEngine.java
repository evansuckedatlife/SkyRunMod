package com.skyrunmod.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core split and analytics model for SkyRunMod.
 */
public final class SkyRunAnalyticsEngine {
    private static final Pattern CRYSTAL_PICKUP_PATTERN =
            Pattern.compile("^([A-Za-z0-9_]{1,16}) picked up an energy crystal!$");
    private static final Pattern TERMINAL_PATTERN =
            Pattern.compile("^([A-Za-z0-9_]{1,16}) completed a terminal(?: \\((\\d+)/(\\d+)\\))?!$");
    private static final Pattern DRAGON_KILL_PATTERN =
            Pattern.compile("^([A-Za-z0-9_]{1,16}) killed (\\w+) dragon!$");
    private static final Pattern RELIC_PATTERN =
            Pattern.compile("^([A-Za-z0-9_]{1,16}) picked up a relic!$");

    private final Map<String, Long> activeTimers = new HashMap<>();
    private final Map<String, Long> personalBests = new HashMap<>();
    private final Map<String, Double> emaByKey = new HashMap<>();
    private final List<SplitRecord> recentSplits = new ArrayList<>();
    private final OverlayModel overlayModel = new OverlayModel();

    private final double emaAlpha;

    public SkyRunAnalyticsEngine() {
        this(0.30d);
    }

    public SkyRunAnalyticsEngine(double emaAlpha) {
        if (emaAlpha <= 0d || emaAlpha > 1d) {
            throw new IllegalArgumentException("emaAlpha must be in (0, 1]");
        }
        this.emaAlpha = emaAlpha;
    }

    public OverlayModel overlayModel() {
        return overlayModel;
    }

    public List<SplitRecord> recentSplits() {
        return Collections.unmodifiableList(recentSplits);
    }

    public Optional<Long> personalBestMillis(String key) {
        return Optional.ofNullable(personalBests.get(key));
    }

    public Optional<Double> emaMillis(String key) {
        return Optional.ofNullable(emaByKey.get(key));
    }

    public void onChatMessage(String message, long nowMillis) {
        Objects.requireNonNull(message, "message");
        Matcher crystal = CRYSTAL_PICKUP_PATTERN.matcher(message);
        if (crystal.matches()) {
            String player = crystal.group(1);
            completeSplit("dungeon.maxor.crystal_pickup", player, nowMillis);
            overlayModel.setActivity(Activity.DUNGEON_MAXOR);
            overlayModel.setSection("maxor", "Crystal by " + player);
            return;
        }

        Matcher terminal = TERMINAL_PATTERN.matcher(message);
        if (terminal.matches()) {
            String player = terminal.group(1);
            String terminalCount = terminal.group(2) == null
                    ? "?"
                    : terminal.group(2) + "/" + terminal.group(3);
            recordGoldorTerminal(player, terminalCount, nowMillis);
            return;
        }

        Matcher dragonKill = DRAGON_KILL_PATTERN.matcher(message);
        if (dragonKill.matches()) {
            String player = dragonKill.group(1);
            String dragon = dragonKill.group(2).toLowerCase();
            completeSplit("dungeon.m7.dragon." + dragon + ".kill", player, nowMillis);
            overlayModel.setActivity(Activity.DUNGEON_M7_DRAGON);
            overlayModel.setSection("dragon", dragon + " killed by " + player);
            return;
        }

        Matcher relic = RELIC_PATTERN.matcher(message);
        if (relic.matches()) {
            String player = relic.group(1);
            completeSplit("dungeon.m7.relic_pickup", player, nowMillis);
            overlayModel.setActivity(Activity.DUNGEON_M7_DRAGON);
            overlayModel.setSection("dragon", "Relic by " + player);
        }
    }

    public void startSplit(String key, long nowMillis) {
        activeTimers.put(key, nowMillis);
    }

    public Optional<SplitRecord> completeSplit(String key, String actor, long nowMillis) {
        Long start = activeTimers.remove(key);
        if (start == null) {
            return Optional.empty();
        }
        if (nowMillis < start) {
            throw new IllegalArgumentException("nowMillis cannot be earlier than split start for " + key);
        }
        long elapsed = nowMillis - start;
        boolean isPb = updatePersonalBest(key, elapsed);
        double ema = updateEma(key, elapsed);
        SplitRecord record = new SplitRecord(key, actor, elapsed, isPb, ema, nowMillis);
        recentSplits.add(record);
        return Optional.of(record);
    }

    public void startStorm(long nowMillis) {
        startSplit("dungeon.storm.phase", nowMillis);
        overlayModel.setActivity(Activity.DUNGEON_STORM);
        overlayModel.setSection("storm", "Storm started");
    }

    public Optional<SplitRecord> completeStorm(String actor, long nowMillis) {
        Optional<SplitRecord> split = completeSplit("dungeon.storm.phase", actor, nowMillis);
        split.ifPresent(s -> overlayModel.setSection("storm", "Storm crushed: " + s.elapsedMillis() + "ms"));
        return split;
    }

    public void startGoldor(long nowMillis) {
        startSplit("dungeon.goldor.phase", nowMillis);
        overlayModel.setActivity(Activity.DUNGEON_GOLDOR);
        overlayModel.setSection("goldor", "Goldor started");
    }

    public Optional<SplitRecord> completeGoldor(String actor, long nowMillis) {
        Optional<SplitRecord> split = completeSplit("dungeon.goldor.phase", actor, nowMillis);
        split.ifPresent(s -> overlayModel.setSection("goldor", "Goldor done: " + s.elapsedMillis() + "ms"));
        return split;
    }

    public Optional<SplitRecord> recordGoldorTerminal(String actor, String terminalIndex, long nowMillis) {
        if (!activeTimers.containsKey("dungeon.goldor.phase")) {
            return Optional.empty();
        }
        String key = "dungeon.goldor.terminal." + terminalIndex;
        startSplit(key, activeTimers.get("dungeon.goldor.phase"));
        Optional<SplitRecord> split = completeSplit(key, actor, nowMillis);
        split.ifPresent(s -> overlayModel.setSection("goldor", "Terminal " + terminalIndex + " by " + actor));
        return split;
    }

    public void roomEnter(String roomId, long nowMillis) {
        startSplit("room." + roomId, nowMillis);
        overlayModel.setActivity(Activity.ROOM_CLEAR);
        overlayModel.setSection("room", "Entered " + roomId);
    }

    public Optional<SplitRecord> roomExit(String roomId, String actor, long nowMillis) {
        Optional<SplitRecord> split = completeSplit("room." + roomId, actor, nowMillis);
        split.ifPresent(s -> overlayModel.setSection("room", roomId + " clear: " + s.elapsedMillis() + "ms"));
        return split;
    }

    public void routeStart(String roomId, String routeName, long nowMillis) {
        startSplit("route." + roomId + "." + routeName, nowMillis);
        overlayModel.setActivity(Activity.ROUTE_EXECUTION);
        overlayModel.setSection("route", roomId + " / " + routeName + " started");
    }

    public Optional<SplitRecord> routeFinish(String roomId, String routeName, String actor, long nowMillis) {
        Optional<SplitRecord> split = completeSplit("route." + roomId + "." + routeName, actor, nowMillis);
        split.ifPresent(s -> overlayModel.setSection("route", routeName + ": " + s.elapsedMillis() + "ms"));
        return split;
    }

    public void commissionStart(String commissionName, long nowMillis) {
        startSplit("commission." + commissionName, nowMillis);
        overlayModel.setActivity(Activity.COMMISSION);
        overlayModel.setSection("commission", commissionName + " started");
    }

    public Optional<SplitRecord> commissionComplete(String commissionName, String actor, long nowMillis) {
        Optional<SplitRecord> split = completeSplit("commission." + commissionName, actor, nowMillis);
        split.ifPresent(s -> overlayModel.setSection("commission", commissionName + ": " + s.elapsedMillis() + "ms"));
        return split;
    }

    public void transitionStart(String transitionName, long nowMillis) {
        startSplit("transition." + transitionName, nowMillis);
        overlayModel.setActivity(Activity.TRANSITION);
        overlayModel.setSection("transition", transitionName + " started");
    }

    public Optional<SplitRecord> transitionComplete(String transitionName, String actor, long nowMillis) {
        Optional<SplitRecord> split = completeSplit("transition." + transitionName, actor, nowMillis);
        split.ifPresent(s -> overlayModel.setSection("transition", transitionName + ": " + s.elapsedMillis() + "ms"));
        return split;
    }

    private boolean updatePersonalBest(String key, long elapsedMillis) {
        Long pb = personalBests.get(key);
        if (pb == null || elapsedMillis < pb) {
            personalBests.put(key, elapsedMillis);
            return true;
        }
        return false;
    }

    private double updateEma(String key, long elapsedMillis) {
        Double current = emaByKey.get(key);
        double next = (current == null) ? elapsedMillis : (emaAlpha * elapsedMillis) + ((1 - emaAlpha) * current);
        emaByKey.put(key, next);
        return next;
    }

    public enum Activity {
        IDLE,
        DUNGEON_MAXOR,
        DUNGEON_STORM,
        DUNGEON_GOLDOR,
        DUNGEON_M7_DRAGON,
        ROOM_CLEAR,
        ROUTE_EXECUTION,
        COMMISSION,
        TRANSITION
    }

    public static final class OverlayModel {
        private Activity activity = Activity.IDLE;
        private final Map<String, String> sections = new LinkedHashMap<>();

        public Activity activity() {
            return activity;
        }

        public Map<String, String> sections() {
            return Collections.unmodifiableMap(sections);
        }

        public void setActivity(Activity nextActivity) {
            if (activity != nextActivity) {
                sections.clear();
            }
            activity = nextActivity;
        }

        public void setSection(String key, String value) {
            sections.put(key, value);
        }
    }

    public record SplitRecord(
            String key,
            String actor,
            long elapsedMillis,
            boolean personalBest,
            double emaMillis,
            long completedAtMillis) {
    }
}
