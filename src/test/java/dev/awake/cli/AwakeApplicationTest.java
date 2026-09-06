package dev.awake.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AwakeApplicationTest {
    @Test
    void successfulStubRunReturnsZero() {
        Output output = run("--mode", "inhibit", "--hours", "1");

        assertEquals(ExitCode.SUCCESS, output.code);
        assertTrue(output.stdout.contains("Scheduler accepted"));
    }

    @Test
    void invalidArgumentsReturnStableCode() {
        Output output = run("--mode", "activity", "--hours", "1");

        assertEquals(ExitCode.INVALID_ARGUMENTS, output.code);
        assertTrue(output.stderr.contains("requires --target"));
    }

    private Output run(String... args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        Clock clock = Clock.fixed(Instant.parse("2026-09-06T08:00:00Z"), ZoneOffset.UTC);
        int code = AwakeApplication.run(
                args,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8),
                clock,
                (options, window, out) -> {
                    out.println("Scheduler accepted " + window.getActiveDuration());
                    return ExitCode.SUCCESS;
                });
        return new Output(code, stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
    }

    private static final class Output {
        private final int code;
        private final String stdout;
        private final String stderr;

        private Output(int code, String stdout, String stderr) {
            this.code = code;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }
}
