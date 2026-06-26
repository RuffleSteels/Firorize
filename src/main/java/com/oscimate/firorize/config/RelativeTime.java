package com.oscimate.firorize.config;

import net.minecraft.network.chat.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Formats a SQLite UTC timestamp ({@code yyyy-MM-dd HH:mm:ss}, as produced by D1's
 * {@code CURRENT_TIMESTAMP}) into a localized "x ago" string for the online preset list.
 */
public final class RelativeTime {
    private RelativeTime() {}

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Component format(String dateCreated) {
        if (dateCreated == null || dateCreated.isBlank()) {
            return Component.translatable("firorize.time.justNow");
        }
        Instant then;
        try {
            String normalized = dateCreated.replace('T', ' ').trim();
            if (normalized.length() > 19) normalized = normalized.substring(0, 19);
            then = LocalDateTime.parse(normalized, FMT).toInstant(ZoneOffset.UTC);
        } catch (RuntimeException e) {
            return Component.literal(dateCreated);
        }

        long sec = Duration.between(then, Instant.now()).getSeconds();
        if (sec < 0) sec = 0;
        if (sec < 45) return Component.translatable("firorize.time.justNow");

        long minutes = sec / 60;
        if (minutes < 60) return unit(minutes, "minute");
        long hours = minutes / 60;
        if (hours < 24) return unit(hours, "hour");
        long days = hours / 24;
        if (days < 7) return unit(days, "day");
        long weeks = days / 7;
        if (weeks < 5) return unit(weeks, "week");
        long months = days / 30;
        if (months < 12) return unit(months, "month");
        return unit(days / 365, "year");
    }

    private static Component unit(long n, String name) {
        if (n <= 1) return Component.translatable("firorize.time." + name + "Ago");
        return Component.translatable("firorize.time." + name + "sAgo", n);
    }
}
