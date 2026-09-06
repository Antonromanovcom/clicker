package dev.awake.scheduling;

import dev.awake.cli.CliOptions;
import dev.awake.cli.ExitCode;
import java.io.PrintStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

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
    public int execute(CliOptions options, TimeWindow window, PrintStream out) {
        CancellationToken cancellation = new CancellationToken();
        Thread shutdownHook = new Thread(() -> {
            cancellation.cancel();
            out.println("Stopped: JVM shutdown requested (for example, Ctrl+C).");
        }, "awake-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        try {
            printPlan(options, window, out);
            if (!waitForStart(window, cancellation)) {
                out.println("Stopped before start: interrupted by user.");
                return ExitCode.INTERRUPTED;
            }

            out.println("Started: " + options.getMode().cliValue() + " mode placeholder.");
            if (!waitActiveDuration(window.getActiveDuration(), cancellation)) {
                out.println("Stopped: interrupted by user.");
                return ExitCode.INTERRUPTED;
            }
            out.println("Finished: planned time window completed.");
            return ExitCode.SUCCESS;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            out.println("Stopped: thread interrupted.");
            return ExitCode.INTERRUPTED;
        } finally {
            removeShutdownHook(shutdownHook);
        }
    }

    private void printPlan(CliOptions options, TimeWindow window, PrintStream out) {
        out.println("Mode: " + options.getMode().cliValue());
        out.println("Starts: " + DISPLAY_TIME.format(window.getStart()));
        out.println("Ends:   " + DISPLAY_TIME.format(window.getEnd()));
        out.println("Behavior: placeholder only; no sleep prevention or input emulation is active.");
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

    private boolean waitActiveDuration(Duration duration, CancellationToken cancellation)
            throws InterruptedException {
        long durationNanos = duration.toNanos();
        long startedAt = monotonicClock.nanoTime();
        while (!cancellation.isCancelled()) {
            long elapsed = monotonicClock.nanoTime() - startedAt;
            long remaining = durationNanos - elapsed;
            if (remaining <= 0) {
                return true;
            }
            sleeper.sleep(shorterOf(Duration.ofNanos(remaining), MAX_SLEEP_SLICE));
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
}
