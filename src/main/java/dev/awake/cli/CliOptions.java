package dev.awake.cli;

import java.nio.file.Path;
import java.time.LocalTime;

public final class CliOptions {
    private final AwakeMode mode;
    private final Long hours;
    private final LocalTime from;
    private final LocalTime until;
    private final Path target;
    private final long intervalSeconds;

    CliOptions(AwakeMode mode, Long hours, LocalTime from, LocalTime until,
               Path target, long intervalSeconds) {
        this.mode = mode;
        this.hours = hours;
        this.from = from;
        this.until = until;
        this.target = target;
        this.intervalSeconds = intervalSeconds;
    }

    public AwakeMode getMode() {
        return mode;
    }

    public Long getHours() {
        return hours;
    }

    public LocalTime getFrom() {
        return from;
    }

    public LocalTime getUntil() {
        return until;
    }

    public Path getTarget() {
        return target;
    }

    public long getIntervalSeconds() {
        return intervalSeconds;
    }
}

