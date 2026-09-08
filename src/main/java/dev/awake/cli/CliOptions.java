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
    private final boolean inhibitDisabled;
    private final boolean displayAwake;
    private final Long eraseAfter;
    private final boolean dryRun;
    private final LogLevel logLevel;

    CliOptions(AwakeMode mode, Long hours, LocalTime from, LocalTime until,
               Path target, long intervalSeconds, boolean inhibitDisabled,
               boolean displayAwake, Long eraseAfter, boolean dryRun, LogLevel logLevel) {
        this.mode = mode;
        this.hours = hours;
        this.from = from;
        this.until = until;
        this.target = target;
        this.intervalSeconds = intervalSeconds;
        this.inhibitDisabled = inhibitDisabled;
        this.displayAwake = displayAwake;
        this.eraseAfter = eraseAfter;
        this.dryRun = dryRun;
        this.logLevel = logLevel;
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

    public boolean isInhibitDisabled() {
        return inhibitDisabled;
    }

    public boolean isDisplayAwake() {
        return displayAwake;
    }

    public Long getEraseAfter() {
        return eraseAfter;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }
}
