package dev.awake.scheduling;

import dev.awake.cli.CliOptions;
import dev.awake.cli.ExitCode;
import dev.awake.mode.ModeException;
import dev.awake.mode.WakefulnessMode;
import java.io.PrintStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public final class AwakeScheduler implements ExecutionScheduler {
    private static final Duration MAX_SLEEP_SLICE = Duration.ofMillis(250);
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss VV");

    private final Clock wallClock;
    private final MonotonicClock monotonicClock;
    private final Sleeper sleeper;

    public AwakeScheduler(Clock wallClock) {
        this(wallClock, System::nanoTime, duration -> {
            long millis = duration.toMillis();
            int nanos = (int) (duration.minusMillis(millis).toNanos());
            Thread.sleep(millis, nanos);
        });
    }

    AwakeScheduler(Clock wallClock, MonotonicClock monotonicClock, Sleeper sleeper) {
        this.wallClock = wallClock;
        this.monotonicClock = monotonicClock;
        this.sleeper = sleeper;
    }

    @Override
    public int execute(CliOptions options, TimeWindow window, WakefulnessMode mode, PrintStream out) {
        CancellationToken cancellation = new CancellationToken();
        Thread shutdownHook = new Thread(() -> {
            cancellation.cancel();
            stopMode(mode, out);
            out.println("Stopped: JVM shutdown requested (for example, Ctrl+C).");
        }, "awake-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        try {
            printPlan(options, window, mode, out);
            if (!waitForStart(window, cancellation)) {
                out.println("Stopped before start: interrupted by user.");
                return ExitCode.INTERRUPTED;
            }

            mode.start();
            out.println("Started: " + mode.description() + ".");
            if (!waitActiveDuration(window.getActiveDuration(), options.getIntervalSeconds(), mode, cancellation)) {
                out.println("Stopped: interrupted by user.");
                return ExitCode.INTERRUPTED;
            }
            out.println("Finished: planned time window completed.");
            return ExitCode.SUCCESS;
        } catch (ModeException exception) {
            out.println("Mode error: " + exception.getMessage());
            return ExitCode.PLATFORM_ERROR;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            out.println("Stopped: thread interrupted.");
            return ExitCode.INTERRUPTED;
        } finally {
            stopMode(mode, out);
            removeShutdownHook(shutdownHook);
        }
    }

    private void printPlan(CliOptions options, TimeWindow window, WakefulnessMode mode, PrintStream out) {
        out.println("Mode: " + options.getMode().cliValue());
        out.println("Starts: " + DISPLAY_TIME.format(window.getStart()));
        out.println("Ends:   " + DISPLAY_TIME.format(window.getEnd()));
        out.println("Behavior: " + mode.description() + ".");
    }

    private boolean waitForStart(TimeWindow window, CancellationToken cancellation)
            throws InterruptedException {
        while (!cancellation.isCancelled()) {
            Duration remaining = Duration.between(wallClock.instant(), window.getStart().toInstant());
            if (remaining.isZero() || remaining.isNegative()) {
                return true;
            }
            sleeper.sleep(shorterOf(remaining, MAX_SLEEP_SLICE));
        }
        return false;
    }

    private boolean waitActiveDuration(Duration duration, long intervalSeconds,
                                       WakefulnessMode mode, CancellationToken cancellation)
            throws InterruptedException, ModeException {
        long durationNanos = duration.toNanos();
        long intervalNanos = Duration.ofSeconds(intervalSeconds).toNanos();
        long startedAt = monotonicClock.nanoTime();
        long nextPulse = startedAt;
        while (!cancellation.isCancelled()) {
            long elapsed = monotonicClock.nanoTime() - startedAt;
            long remaining = durationNanos - elapsed;
            if (remaining <= 0) {
                return true;
            }
            long now = monotonicClock.nanoTime();
            if (now >= nextPulse) {
                mode.pulse();
                nextPulse = now + intervalNanos;
            }
            long untilPulse = Math.max(1, nextPulse - monotonicClock.nanoTime());
            sleeper.sleep(shorterOf(
                    shorterOf(Duration.ofNanos(remaining), Duration.ofNanos(untilPulse)),
                    MAX_SLEEP_SLICE));
        }
        return false;
    }

    private Duration shorterOf(Duration first, Duration second) {
        return first.compareTo(second) <= 0 ? first : second;
    }

    private void removeShutdownHook(Thread hook) {
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException ignored) {
            // The JVM is already shutting down and is executing the hook.
        }
    }

    private static void stopMode(WakefulnessMode mode, PrintStream out) {
        try {
            mode.stop();
        } catch (ModeException exception) {
            out.println("Mode cleanup error: " + exception.getMessage());
        }
    }
}
