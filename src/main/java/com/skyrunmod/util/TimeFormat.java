package com.skyrunmod.util;

/**
 * Speedrun-style duration formatting. Pure helper, no Minecraft dependency, so it is covered by the
 * core unit tests alongside {@link com.skyrunmod.core.SkyRunAnalyticsEngine}.
 */
public final class TimeFormat {
    private TimeFormat() {
    }

    /**
     * Formats a duration as a LiveSplit-style clock:
     * <ul>
     *   <li>{@code < 1 min} → {@code S.mmm} (e.g. {@code 4.207})</li>
     *   <li>{@code < 1 hr}  → {@code M:SS.mmm} (e.g. {@code 1:04.207})</li>
     *   <li>otherwise       → {@code H:MM:SS.mmm}</li>
     * </ul>
     */
    public static String duration(long millis) {
        if (millis < 0) {
            return "-" + duration(-millis);
        }
        long ms = millis % 1000;
        long totalSeconds = millis / 1000;
        long seconds = totalSeconds % 60;
        long totalMinutes = totalSeconds / 60;
        long minutes = totalMinutes % 60;
        long hours = totalMinutes / 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, ms);
        }
        if (totalMinutes > 0) {
            return String.format("%d:%02d.%03d", minutes, seconds, ms);
        }
        return String.format("%d.%03d", seconds, ms);
    }

    /** Formats a signed delta with an explicit {@code +}/{@code -} sign, e.g. {@code -1.204}. */
    public static String signedDelta(long deltaMillis) {
        String sign = deltaMillis > 0 ? "+" : "-";
        return sign + duration(Math.abs(deltaMillis));
    }
}
