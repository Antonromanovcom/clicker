package dev.awake.mode;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.awake.cli.CliOptions;
import dev.awake.cli.CliParser;
import org.junit.jupiter.api.Test;

class ModeFactoryTest {
    @Test
    void createsMacOsInhibitBackend() throws Exception {
        WakefulnessMode mode = factory("Mac OS X").create(options(
                "--mode", "inhibit", "--hours", "1"));

        assertTrue(mode.description().contains("caffeinate"));
    }

    @Test
    void createsActivityPlaceholder() throws Exception {
        WakefulnessMode mode = factory("Linux").create(options(
                "--mode", "activity", "--hours", "1", "--target", "/tmp/awake.txt"));

        assertTrue(mode.description().contains("activity placeholder"));
        assertTrue(mode.description().contains("systemd-inhibit"));
    }

    @Test
    void noInhibitCreatesActivityOnly() throws Exception {
        WakefulnessMode mode = factory("Linux").create(options(
                "--mode", "activity", "--hours", "1", "--target", "/tmp/awake.txt",
                "--no-inhibit", "--erase-after", "100"));

        assertTrue(mode.description().contains("batch of 100"));
        assertTrue(!mode.description().contains("systemd-inhibit"));
    }

    @Test
    void dryRunHasNoInhibitSideEffect() throws Exception {
        WakefulnessMode mode = factory("Mac OS X").create(options(
                "--mode", "activity", "--hours", "1", "--target", "/tmp/awake.txt", "--dry-run"));

        assertTrue(mode.description().contains("activity dry-run"));
        assertTrue(!mode.description().contains("caffeinate"));
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
