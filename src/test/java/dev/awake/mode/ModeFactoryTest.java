package dev.awake.mode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.awake.cli.CliOptions;
import dev.awake.cli.CliParser;
import org.junit.jupiter.api.Test;

class ModeFactoryTest {
    @Test
    void createsInhibitPlaceholderWithIdempotentStop() throws Exception {
        WakefulnessMode mode = factory("Mac OS X").create(options(
                "--mode", "inhibit", "--hours", "1"));

        assertFalse(mode.isRunning());
        mode.start();
        assertTrue(mode.isRunning());
        mode.stop();
        mode.stop();
        assertFalse(mode.isRunning());
    }

    @Test
    void createsActivityPlaceholder() throws Exception {
        WakefulnessMode mode = factory("Linux").create(options(
                "--mode", "activity", "--hours", "1", "--target", "/tmp/awake.txt"));

        assertTrue(mode.description().contains("activity placeholder"));
    }

    @Test
    void rejectsUnsupportedPlatform() throws Exception {
        CliOptions options = options("--mode", "inhibit", "--hours", "1");

        assertThrows(ModeException.class, () -> factory("Plan 9").create(options));
    }

    private ModeFactory factory(String osName) {
        return new ModeFactory(new PlatformDetector(() -> osName));
    }

    private CliOptions options(String... args) throws Exception {
        return new CliParser().parse(args).getOptions();
    }
}

