package com.company.scopehandler.api.services;

public final class DurationFormatter {
    private DurationFormatter() {
    }

    public static String formatSeconds(long totalSeconds) {
        if (totalSeconds < 0) {
            return "-";
        }
        if (totalSeconds < 60) {
            return totalSeconds + "s";
        }
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        if (minutes < 60) {
            return minutes + "m" + seconds + "s";
        }
        long hours = minutes / 60;
        minutes = minutes % 60;
        if (hours < 24) {
            return hours + "h" + minutes + "m" + seconds + "s";
        }
        long days = hours / 24;
        hours = hours % 24;
        return days + "d" + hours + "h" + minutes + "m" + seconds + "s";
    }
}
