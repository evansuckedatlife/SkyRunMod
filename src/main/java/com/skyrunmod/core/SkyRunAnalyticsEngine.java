package com.skyrunmod.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core split and analytics model for SkyRunMod.
 *
 * <p>This class is intentionally free of any Minecraft / Fabric types so it can be unit tested on a
 * plain JVM and reasoned about in isolation. The client layer ({@code com.skyrunmod.client}) feeds it
 * stripped chat lines and wall-clock timestamps, and renders {@link OverlayModel} / {@link #recentSplits()}.
 *
 * <p>Timestamps are caller supplied milliseconds. Callers should pass a <em>monotonic</em> clock
 * (e.g. derived from {@link System#nanoTime()}) so that NTP corrections cannot make a split go
 * backwards; {@link #completeSplit} rejects negative durations defensively.
 */
public final class SkyRunAnalyticsEngine {
    /** Upper bound on retained split history; older entries are evicted FIFO. */
    public static final int MAX_RECENT_SPLITS = 200;

    private static final String USERNAME_REGEX = "[A-Za-z0-9_]{1,16}";
    private static final Pattern CRYSTAL_PICKUP_PATTERN =
            Pattern.compile("^(" + USERNAME_REGEX + ") picked up an energy crystal!$");
    private static final Pattern CRYSTAL_PLACED_PATTERN =
            Pattern.compile("^(" + USERNAME_REGEX + ") placed an energy crystal!$");
    // Goldor phase objectives. Hypixel uses both "activated" and "completed" wording across
    // terminals/devices/levers, with an optional "(done/total)" progress suffix.
    private static final Pattern GOLDOR_OBJECTIVE_PATTERN =
            Pattern.compile("^(" + USERNAME_REGEX
                    + ") (?:completed|activated) a (terminal|device|lever)(?: \\((\\d+)/(\\d+)\\))?!$");
    private static final Pattern DRAGON_KILL_PATTERN =
            Pattern.compile("^(" + USERNAME_REGEX + ") killed (\\w+) dragon!$");
    private static final Pattern RELIC_PATTERN =
            Pattern.compile("^(" + USERNAME_REGEX + ") picked up a relic!$");

    private final Map<String, Long> activeTimers = new HashMap<>();
    private final Map<String, Long> personalBests = new HashMap<>();
    private final Map<String, Double> emaByKey = new HashMap<>();
    private final Deque<SplitRecord> recentSplits = new ArrayDeque<>();
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

    /** Retained split history, oldest first, capped at {@link #MAX_RECENT_SPLITS}. */
    public List<SplitRecord> recentSplits() {
        return Collections.unmodifiableList(new ArrayList<>(recentSplits));
    }

    /** Most recent split, if any has been recorded. */
    public Optional<SplitRecord> lastSplit() {
        return Optional.ofNullable(recentSplits.peekLast());
    }

    public Optional<Long> personalBestMillis(String key) {
        return Optional.ofNullable(personalBests.get(key));
    }

    public Optional<Double> emaMillis(String key) {
        return Optional.ofNullable(emaByKey.get(key));
    }

    /** True while a timer for {@code key} is running (started but not yet completed). */
    public boolean isTracking(String key) {
        return activeTimers.containsKey(key);
    }

    /** Snapshot of all personal bests, for persistence. */
    public Map<String, Long> personalBests() {
        return Collections.unmodifiableMap(new HashMap<>(personalBests));
    }

    /** Snapshot of all EMAs, for persistence. */
    public Map<String, Double> emaSnapshot() {
        return Collections.unmodifiableMap(new HashMap<>(emaByKey));
    }

    /**
     * Replaces the persisted analytics (PBs + EMAs) with the supplied maps. Active timers and the
     * overlay are left untouched, so this is safe to call at startup before a run begins.
     */
    public void restore(Map<String, Long> personalBests, Map<String, Double> emas) {
        this.personalBests.clear();
        if (personalBests != null) {
            personalBests.forEach((k, v) -> {
                if (k != null && v != null && v >= 0L) {
                    this.personalBests.put(k, v);
                }
            });
        }
        this.emaByKey.clear();
        if (emas != null) {
            emas.forEach((k, v) -> {
                if (k != null && v != null && v >= 0d) {
                    this.emaByKey.put(k, v);
                }
            });
        }
    }

    /**
     * Sum of personal bests for every key beginning with {@code keyPrefix} — the "sum of best"
     * (theoretical best run) used by speedrun overlays. Returns empty if no PB matches.
     */
    public OptionalLong sumOfBest(String keyPrefix) {
        Objects.requireNonNull(keyPrefix, "keyPrefix");
        long sum = 0L;
        boolean any = false;
        for (Map.Entry<String, Long> entry : personalBests.entrySet()) {
            if (entry.getKey().startsWith(keyPrefix)) {
                sum += entry.getValue();
                any = true;
            }
        }
        return any ? OptionalLong.of(sum) : OptionalLong.empty();
    }

    /**
     * Clears in-flight timers, split history and the overlay for a fresh run. Personal bests and
     * EMAs are kept so comparisons survive across runs.
     */
    public void resetRun() {
        activeTimers.clear();
        recentSplits.clear();
        overlayModel.reset();
    }

    public void onChatMessage(String message, long nowMillis) {
        Objects.requireNonNull(message, "message");

        Matcher crystal = CRYSTAL_PICKUP_PATTERN.matcher(message);
        if (crystal.matches()) {
            String player = crystal.group(1);
            completeSplit("dungeon.maxor.crystal_pickup", player, nowMillis);
            overlayModel.setActivity(Activity.DUNGEON_MAXOR);
            overlayModel.setSection("maxor", "Crystal by " + player, nowMillis);
            return;
        }

        Matcher crystalPlaced = CRYSTAL_PLACED_PATTERN.matcher(message);
        if (crystalPlaced.matches()) {
            String player = crystalPlaced.group(1);
            completeSplit("dungeon.maxor.crystal_place", player, nowMillis);
            overlayModel.setActivity(Activity.DUNGEON_MAXOR);
            overlayModel.setSection("maxor", "Crystal placed by " + player, nowMillis);
            return;
        }

        Matcher objective = GOLDOR_OBJECTIVE_PATTERN.matcher(message);
        if (objective.matches()) {
            String player = objective.group(1);
            String type = objective.group(2);
            String index = objective.group(3) == null
                    ? "?"
                    : objective.group(3) + "/" + objective.group(4);
            recordGoldorObjective(type, index, player, nowMillis);
            return;
        }

        Matcher dragonKill = DRAGON_KILL_PATTERN.matcher(message);
        if (dragonKill.matches()) {
            String player = dragonKill.group(1);
            String dragon = dragonKill.group(2).toLowerCase();
            completeSplit("dungeon.m7.dragon." + dragon + ".kill", player, nowMillis);
            overlayModel.setActivity(Activity.DUNGEON_M7_DRAGON);
            overlayModel.setSection("dragon", dragon + " killed by " + player, nowMillis);
            return;
        }

        Matcher relic = RELIC_PATTERN.matcher(message);
        if (relic.matches()) {
            String player = relic.group(1);
            completeSplit("dungeon.m7.relic_pickup", player, nowMillis);
            overlayModel.setActivity(Activity.DUNGEON_M7_DRAGON);
            overlayModel.setSection("dragon", "Relic by " + player, nowMillis);
        }
    }

    public void startSplit(String key, long nowMillis) {
        activeTimers.put(key, nowMillis);
    }

    /** Discards a running timer without recording a split (e.g. a route was abandoned). */
    public boolean cancelSplit(String key) {
        return activeTimers.remove(key) != null;
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
        Long previousBest = personalBests.get(key);
        boolean isPb = updatePersonalBest(key, elapsed);
        double ema = updateEma(key, elapsed);
        SplitRecord record = new SplitRecord(key, actor, elapsed, isPb, ema, nowMillis, previousBest);
        recentSplits.addLast(record);
        while (recentSplits.size() > MAX_RECENT_SPLITS) {
            recentSplits.removeFirst();
        }
        return Optional.of(record);
    }

    public void startMaxor(long nowMillis) {
        startSplit("dungeon.maxor.phase", nowMillis);
        startSplit("dungeon.maxor.crystal_pickup", nowMillis);
        overlayModel.setActivity(Activity.DUNGEON_MAXOR);
        overlayModel.setSection("maxor", "Maxor started", nowMillis);
    }

    public void startStorm(long nowMillis) {
        startSplit("dungeon.storm.phase", nowMillis);
        overlayModel.setActivity(Activity.DUNGEON_STORM);
        overlayModel.setSection("storm", "Storm started", nowMillis);
    }

    public Optional<SplitRecord> completeStorm(String actor, long nowMillis) {
        Optional<SplitRecord> split = completeSplit("dungeon.storm.phase", actor, nowMillis);
        split.ifPresent(s -> overlayModel.setSection("storm", "Storm crushed: " + s.elapsedMillis() + "ms", nowMillis));
        return split;
    }

    public void startGoldor(long nowMillis) {
        startSplit("dungeon.goldor.phase", nowMillis);
        overlayModel.setActivity(Activity.DUNGEON_GOLDOR);
        overlayModel.setSection("goldor", "Goldor started", nowMillis);
    }

    public Optional<SplitRecord> completeGoldor(String actor, long nowMillis) {
        Optional<SplitRecord> split = completeSplit("dungeon.goldor.phase", actor, nowMillis);
        split.ifPresent(s -> overlayModel.setSection("goldor", "Goldor done: " + s.elapsedMillis() + "ms", nowMillis));
        return split;
    }

    /** Back-compat shim for the original terminal-only API; delegates to {@link #recordGoldorObjective}. */
    public Optional<SplitRecord> recordGoldorTerminal(String actor, String terminalIndex, long nowMillis) {
        return recordGoldorObjective("terminal", terminalIndex, actor, nowMillis);
    }

    /**
     * Records the time from Goldor-phase start to the completion of a terminal/device/lever. Returns
     * empty (and records nothing) if the Goldor phase isn't currently being tracked, so stray chat
     * outside a run is ignored.
     */
    public Optional<SplitRecord> recordGoldorObjective(String type, String index, String actor, long nowMillis) {
        Long phaseStart = activeTimers.get("dungeon.goldor.phase");
        if (phaseStart == null) {
            return Optional.empty();
        }
        String key = "dungeon.goldor." + type + "." + index;
        startSplit(key, phaseStart);
        Optional<SplitRecord> split = completeSplit(key, actor, nowMillis);
        split.ifPresent(s -> overlayModel.setSection(
                "goldor", capitalize(type) + " " + index + " by " + actor, nowMillis));
        return split;
    }

    public void roomEnter(String roomId, long nowMillis) {
        startSplit("room." + roomId, nowMillis);
        overlayModel.setActivity(Activity.ROOM_CLEAR);
        overlayModel.setSection("room", "Entered " + roomId, nowMillis);
    }

    public Optional<SplitRecord> roomExit(String roomId, String actor, long nowMillis) {
        Optional<SplitRecord> split = completeSplit("room." + roomId, actor, nowMillis);
        split.ifPresent(s -> overlayModel.setSection("room", roomId + " clear: " + s.elapsedMillis() + "ms", nowMillis));
        return split;
    }

    public void routeStart(String roomId, String routeName, long nowMillis) {
        startSplit("route." + roomId + "." + routeName, nowMillis);
        overlayModel.setActivity(Activity.ROUTE_EXECUTION);
        overlayModel.setSection("route", roomId + " / " + routeName + " started", nowMillis);
    }

    public Optional<SplitRecord> routeFinish(String roomId, String routeName, String actor, long nowMillis) {
        Optional<SplitRecord> split = completeSplit("route." + roomId + "." + routeName, actor, nowMillis);
        split.ifPresent(s -> overlayModel.setSection("route", routeName + ": " + s.elapsedMillis() + "ms", nowMillis));
        return split;
    }

    public void commissionStart(String commissionName, long nowMillis) {
        startSplit("commission." + commissionName, nowMillis);
        overlayModel.setActivity(Activity.COMMISSION);
        overlayModel.setSection("commission", commissionName + " started", nowMillis);
    }

    public Optional<SplitRecord> commissionComplete(String commissionName, String actor, long nowMillis) {
        Optional<SplitRecord> split = completeSplit("commission." + commissionName, actor, nowMillis);
        split.ifPresent(s -> overlayModel.setSection("commission", commissionName + ": " + s.elapsedMillis() + "ms", nowMillis));
        return split;
    }

    public void transitionStart(String transitionName, long nowMillis) {
        startSplit("transition." + transitionName, nowMillis);
        overlayModel.setActivity(Activity.TRANSITION);
        overlayModel.setSection("transition", transitionName + " started", nowMillis);
    }

    public Optional<SplitRecord> transitionComplete(String transitionName, String actor, long nowMillis) {
        Optional<SplitRecord> split = completeSplit("transition." + transitionName, actor, nowMillis);
        split.ifPresent(s -> overlayModel.setSection("transition", transitionName + ": " + s.elapsedMillis() + "ms", nowMillis));
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

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    public enum Activity {
        IDLE,
        DUNGEON_MAXOR,
        DUNGEON_STORM,
        DUNGEON_GOLDOR,
        DUNGEON_M7_DRAGON,
        DUNGEON_FLOOR,
        ROOM_CLEAR,
        ROUTE_EXECUTION,
        COMMISSION,
        TRANSITION,
        SLAYER,
        KUUDRA,
        DIANA,
        MINING_RUN,
        FISHING
    }

    /**
     * Live overlay state. Sections are keyed status lines that reset whenever the tracked activity
     * changes (so a Storm line doesn't linger into a Commission), and can additionally be pruned by
     * age via {@link #pruneStale}.
     */
    public static final class OverlayModel {
        private Activity activity = Activity.IDLE;
        private final Map<String, String> sections = new LinkedHashMap<>();
        private final Map<String, Long> sectionUpdatedAt = new HashMap<>();

        public Activity activity() {
            return activity;
        }

        public Map<String, String> sections() {
            return Collections.unmodifiableMap(sections);
        }

        public void setActivity(Activity nextActivity) {
            if (activity != nextActivity) {
                sections.clear();
                sectionUpdatedAt.clear();
            }
            activity = nextActivity;
        }

        /** Sets a status line without a timestamp; it will never be pruned by {@link #pruneStale}. */
        public void setSection(String key, String value) {
            sections.put(key, value);
            sectionUpdatedAt.remove(key);
        }

        public void setSection(String key, String value, long nowMillis) {
            sections.put(key, value);
            sectionUpdatedAt.put(key, nowMillis);
        }

        /** Removes timestamped sections older than {@code ttlMillis}. */
        public void pruneStale(long nowMillis, long ttlMillis) {
            sectionUpdatedAt.entrySet().removeIf(entry -> {
                if (nowMillis - entry.getValue() >= ttlMillis) {
                    sections.remove(entry.getKey());
                    return true;
                }
                return false;
            });
        }

        public void reset() {
            activity = Activity.IDLE;
            sections.clear();
            sectionUpdatedAt.clear();
        }
    }

    /**
     * One completed split.
     *
     * @param previousBestMillis the personal best for this key <em>before</em> this split, or
     *                           {@code null} if this was the first ever recording.
     */
    public record SplitRecord(
            String key,
            String actor,
            long elapsedMillis,
            boolean personalBest,
            double emaMillis,
            long completedAtMillis,
            Long previousBestMillis) {

        /**
         * Signed delta against the previous personal best (this split minus old PB). Negative means
         * this run was faster than the prior best. Empty when there was no prior best.
         */
        public OptionalLong deltaToPreviousBest() {
            return previousBestMillis == null
                    ? OptionalLong.empty()
                    : OptionalLong.of(elapsedMillis - previousBestMillis);
        }
    }
}
