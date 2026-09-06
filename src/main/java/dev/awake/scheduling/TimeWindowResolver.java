package dev.awake.scheduling;

import dev.awake.cli.CliException;
import dev.awake.cli.CliOptions;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZonedDateTime;

public final class TimeWindowResolver {
    private final Clock clock;

    public TimeWindowResolver(Clock clock) {
        this.clock = clock;
    }

    public TimeWindow resolve(CliOptions options) throws CliException {
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime start = resolveStart(options, now);
        try {
            ZonedDateTime end = options.getHours() != null
                    ? start.plusHours(options.getHours())
                    : resolveEnd(options, start);
            TimeWindow window = new TimeWindow(start, end);
            window.getActiveDuration().toNanos();
            return window;
        } catch (DateTimeException | ArithmeticException exception) {
            throw new CliException("The requested time window is too large.");
        }
    }

    private ZonedDateTime resolveStart(CliOptions options, ZonedDateTime now) {
        if (options.getFrom() == null) {
            return now;
        }
        LocalDate date = now.toLocalDate();
        ZonedDateTime candidate = ZonedDateTime.of(date, options.getFrom(), clock.getZone());
        return candidate.isBefore(now) ? candidate.plusDays(1) : candidate;
    }

    private ZonedDateTime resolveEnd(CliOptions options, ZonedDateTime start) {
        ZonedDateTime end = ZonedDateTime.of(
                start.toLocalDate(), options.getUntil(), clock.getZone());
        return end.toInstant().isAfter(start.toInstant()) ? end : end.plusDays(1);
    }
}
