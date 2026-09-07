package dev.awake.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.awake.cli.CliOptions;
import dev.awake.cli.CliParser;
import dev.awake.cli.ExitCode;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import dev.awake.mode.WakefulnessMode;

class AwakeSchedulerTest {
    @Test
    void waitsForStartThenCompletesUsingMonotonicTime() throws Exception {
        FakeTime time = new FakeTime(Instant.parse("2026-09-06T08:00:00Z"));
        Clock clock = new AdvancingClock(time);
        AwakeScheduler scheduler = new AwakeScheduler(clock, time::nanoTime, time::sleep);
        TimeWindow window = new TimeWindow(
                Instant.parse("2026-09-06T08:00:01Z").atZone(ZoneOffset.UTC),
                Instant.parse("2026-09-06T08:00:03Z").atZone(ZoneOffset.UTC));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        RecordingMode mode = new RecordingMode();
        int result = scheduler.execute(options(), window, mode,
                new PrintStream(bytes, true, StandardCharsets.UTF_8));

        assertEquals(ExitCode.SUCCESS, result);
        assertEquals(Duration.ofSeconds(3), time.totalSlept);
        assertEquals(1, mode.startCalls);
        assertEquals(1, mode.stopCalls);
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("planned time window completed"));
    }

    private static final class RecordingMode implements WakefulnessMode {
        private int startCalls;
        private int stopCalls;
        private boolean running;

        public String description() { return "recording placeholder"; }
        public void start() { startCalls++; running = true; }
        public void stop() { if (running) { stopCalls++; running = false; } }
        public boolean isRunning() { return running; }
    }

    private CliOptions options() throws Exception {
        return new CliParser().parse(new String[]{"--mode", "inhibit", "--hours", "1"}).getOptions();
    }

    private static final class FakeTime {
        private Instant instant;
        private long nanos;
        private Duration totalSlept = Duration.ZERO;

        private FakeTime(Instant instant) {
            this.instant = instant;
        }

        private long nanoTime() {
            return nanos;
        }

        private void sleep(Duration duration) {
            instant = instant.plus(duration);
            nanos += duration.toNanos();
            totalSlept = totalSlept.plus(duration);
        }
    }

    private static final class AdvancingClock extends Clock {
        private final FakeTime time;

        private AdvancingClock(FakeTime time) {
            this.time = time;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return time.instant;
        }
    }
}
