package com.skyrunmod.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic per-session counters with per-hour projection — "X per hour" for any grind. The session
 * clock starts on the first recorded event and can be reset. Minecraft-free, unit tested.
 */
public final class SessionRates {
    private final Map<String, Long> counts = new LinkedHashMap<>();
    private long sessionStartMillis = -1;

    /** Adds {@code amount} to {@code metric}, starting the session clock on the first event. */
    public void record(String metric, long amount, long nowMillis) {
        if (sessionStartMillis < 0) {
            sessionStartMillis = nowMillis;
        }
        counts.merge(metric, amount, Long::sum);
    }

    public long count(String metric) {
        return counts.getOrDefault(metric, 0L);
    }

    /** Events per hour for {@code metric} given the elapsed session time, or 0 before any time passes. */
    public double perHour(String metric, long nowMillis) {
        if (sessionStartMillis < 0) {
            return 0d;
        }
        long elapsed = nowMillis - sessionStartMillis;
        if (elapsed <= 0) {
            return 0d;
        }
        return count(metric) * 3_600_000.0d / elapsed;
    }

    /** Elapsed session time in millis, or 0 if nothing recorded yet. */
    public long elapsedMillis(long nowMillis) {
        return sessionStartMillis < 0 ? 0L : Math.max(0L, nowMillis - sessionStartMillis);
    }

    /** Insertion-ordered snapshot of metric → count. */
    public Map<String, Long> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(counts));
    }

    public boolean isEmpty() {
        return counts.isEmpty();
    }

    public void reset() {
        counts.clear();
        sessionStartMillis = -1;
    }
}
