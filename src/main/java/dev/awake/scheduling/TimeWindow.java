package dev.awake.scheduling;

import java.time.Duration;
import java.time.ZonedDateTime;

public final class TimeWindow {
    private final ZonedDateTime start;
    private final ZonedDateTime end;

    public TimeWindow(ZonedDateTime start, ZonedDateTime end) {
        if (!end.toInstant().isAfter(start.toInstant())) {
            throw new IllegalArgumentException("End must be after start.");
        }
        this.start = start;
        this.end = end;
    }

    public ZonedDateTime getStart() {
        return start;
    }

    public ZonedDateTime getEnd() {
        return end;
    }

    public Duration getActiveDuration() {
        return Duration.between(start.toInstant(), end.toInstant());
    }
}

