package dev.awake.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class CliParserTest {
    private final CliParser parser = new CliParser();

    @Test
    void parsesInhibitForHours() throws Exception {
        CliOptions options = parser.parse(new String[]{"--mode", "inhibit", "--hours", "4"}).getOptions();

        assertEquals(AwakeMode.INHIBIT, options.getMode());
        assertEquals(4L, options.getHours());
        assertEquals(60L, options.getIntervalSeconds());
    }

    @Test
    void parsesScheduledActivity() throws Exception {
        CliOptions options = parser.parse(new String[]{
                "--mode", "activity", "--from", "20:00", "--until", "23:30",
                "--target", "/tmp/awake.txt", "--keypress-interval", "30"}).getOptions();

        assertEquals(AwakeMode.ACTIVITY, options.getMode());
        assertEquals(LocalTime.of(20, 0), options.getFrom());
        assertEquals(LocalTime.of(23, 30), options.getUntil());
        assertEquals(Path.of("/tmp/awake.txt"), options.getTarget());
        assertEquals(30L, options.getIntervalSeconds());
    }

    @Test
    void recognizesHelpAndVersion() throws Exception {
        assertEquals(ParseResult.Action.HELP, parser.parse(new String[]{"--help"}).getAction());
        assertEquals(ParseResult.Action.VERSION, parser.parse(new String[]{"--version"}).getAction());
    }

    @Test
    void rejectsMissingTimeWindow() {
        assertThrows(CliException.class,
                () -> parser.parse(new String[]{"--mode", "inhibit"}));
    }

    @Test
    void rejectsTwoTimeWindowForms() {
        assertThrows(CliException.class,
                () -> parser.parse(new String[]{"--mode", "inhibit", "--hours", "2", "--until", "22:00"}));
    }

    @Test
    void requiresTargetForActivity() {
        assertThrows(CliException.class,
                () -> parser.parse(new String[]{"--mode", "activity", "--hours", "2"}));
    }

    @Test
    void rejectsTargetForInhibit() {
        assertThrows(CliException.class,
                () -> parser.parse(new String[]{"--mode", "inhibit", "--hours", "2", "--target", "/tmp/x"}));
    }

    @Test
    void rejectsInvalidTimeAndNumbers() {
        assertThrows(CliException.class,
                () -> parser.parse(new String[]{"--mode", "inhibit", "--until", "25:00"}));
        assertThrows(CliException.class,
                () -> parser.parse(new String[]{"--mode", "inhibit", "--hours", "0"}));
    }

    @Test
    void rejectsUnknownAndDuplicateOptions() {
        assertThrows(CliException.class,
                () -> parser.parse(new String[]{"--mode", "inhibit", "--hours", "2", "--wat", "x"}));
        assertThrows(CliException.class,
                () -> parser.parse(new String[]{"--mode", "inhibit", "--mode", "activity", "--hours", "2"}));
    }

    @Test
    void parsesActivitySafetyOptions() throws Exception {
        CliOptions options = parser.parse(new String[]{
                "--mode", "activity", "--hours", "2", "--target", "/tmp/awake.txt",
                "--no-inhibit", "--erase-after", "1000", "--dry-run"}).getOptions();

        assertEquals(true, options.isInhibitDisabled());
        assertEquals(1000L, options.getEraseAfter());
        assertEquals(true, options.isDryRun());
    }

    @Test
    void rejectsActivityOptionsInInhibitModeAndOversizedEraseBatch() {
        assertThrows(CliException.class, () -> parser.parse(new String[]{
                "--mode", "inhibit", "--hours", "2", "--no-inhibit"}));
        assertThrows(CliException.class, () -> parser.parse(new String[]{
                "--mode", "activity", "--hours", "2", "--target", "/tmp/x",
                "--erase-after", "1001"}));
    }

    @Test
    void keepsLegacyIntervalAliasAndRejectsBothIntervalNames() throws Exception {
        CliOptions options = parser.parse(new String[]{
                "--mode", "activity", "--hours", "2", "--target", "/tmp/x", "--interval", "15"}).getOptions();

        assertEquals(15L, options.getIntervalSeconds());
        assertThrows(CliException.class, () -> parser.parse(new String[]{
                "--mode", "activity", "--hours", "2", "--target", "/tmp/x",
                "--interval", "15", "--keypress-interval", "30"}));
    }

    @Test
    void parsesDisplayAwakeAndRejectsConflictWithNoInhibit() throws Exception {
        CliOptions options = parser.parse(new String[]{
                "--mode", "activity", "--hours", "2", "--target", "/tmp/x",
                "--keep-display-awake"}).getOptions();

        assertEquals(true, options.isDisplayAwake());
        assertThrows(CliException.class, () -> parser.parse(new String[]{
                "--mode", "activity", "--hours", "2", "--target", "/tmp/x",
                "--keep-display-awake", "--no-inhibit"}));
    }

    @Test
    void parsesLogLevelAndRejectsUnknownValue() throws Exception {
        CliOptions options = parser.parse(new String[]{
                "--mode", "inhibit", "--hours", "2", "--log-level", "verbose"}).getOptions();

        assertEquals(LogLevel.VERBOSE, options.getLogLevel());
        assertThrows(CliException.class, () -> parser.parse(new String[]{
                "--mode", "inhibit", "--hours", "2", "--log-level", "noisy"}));
    }
}
