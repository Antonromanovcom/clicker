package dev.awake.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.awake.cli.CliOptions;
import dev.awake.cli.CliParser;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class TimeWindowResolverTest {
    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");

    @Test
    void hoursStartNowAndHaveExactDuration() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-09-06T07:15:00Z"), MOSCOW);
        TimeWindow window = resolver(clock).resolve(options("--mode", "inhibit", "--hours", "4"));

        assertEquals(Instant.parse("2026-09-06T07:15:00Z"), window.getStart().toInstant());
        assertEquals(Duration.ofHours(4), window.getActiveDuration());
    }

    @Test
    void untilUsesTodayWhenStillAhead() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-09-06T17:00:00Z"), MOSCOW); // 20:00
        TimeWindow window = resolver(clock).resolve(options("--mode", "inhibit", "--until", "23:30"));

        assertEquals(Instant.parse("2026-09-06T20:30:00Z"), window.getEnd().toInstant());
    }

    @Test
    void untilRollsToTomorrowWhenTimeAlreadyPassed() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-09-06T21:00:00Z"), MOSCOW); // 00:00 next day
        TimeWindow window = resolver(clock).resolve(options("--mode", "inhibit", "--until", "23:30"));

        assertEquals(Instant.parse("2026-09-07T20:30:00Z"), window.getEnd().toInstant());
    }

    @Test
    void fromInPastSchedulesNextDayAndEndCanCrossMidnight() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-09-06T18:00:00Z"), MOSCOW); // 21:00
        TimeWindow window = resolver(clock).resolve(options(
                "--mode", "inhibit", "--from", "20:00", "--until", "01:00"));

        assertEquals(Instant.parse("2026-09-07T17:00:00Z"), window.getStart().toInstant());
        assertEquals(Instant.parse("2026-09-07T22:00:00Z"), window.getEnd().toInstant());
        assertEquals(Duration.ofHours(5), window.getActiveDuration());
    }

    @Test
    void durationUsesRealElapsedTimeAcrossDaylightSavingChange() throws Exception {
        ZoneId berlin = ZoneId.of("Europe/Berlin");
        Clock clock = Clock.fixed(Instant.parse("2026-03-28T22:00:00Z"), berlin); // 23:00
        TimeWindow window = resolver(clock).resolve(options("--mode", "inhibit", "--until", "04:00"));

        assertEquals(Duration.ofHours(4), window.getActiveDuration());
    }

    private TimeWindowResolver resolver(Clock clock) {
        return new TimeWindowResolver(clock);
    }

    private CliOptions options(String... args) throws Exception {
        return new CliParser().parse(args).getOptions();
    }
}

