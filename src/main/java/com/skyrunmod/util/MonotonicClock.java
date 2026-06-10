package com.skyrunmod.util;

/**
 * A monotonic millisecond clock backed by {@link System#nanoTime()}.
 *
 * <p>The analytics engine measures durations by subtracting timestamps, so feeding it
 * {@link System#currentTimeMillis()} would let an NTP correction produce a negative split (which the
 * engine rejects with an exception). This source never goes backwards. The zero point is arbitrary —
 * only differences are meaningful — which is exactly what splits need.
 */
public final class MonotonicClock {
    private MonotonicClock() {
    }

    /** Monotonic, non-decreasing milliseconds since an arbitrary origin. */
    public static long millis() {
        return System.nanoTime() / 1_000_000L;
    }
}
