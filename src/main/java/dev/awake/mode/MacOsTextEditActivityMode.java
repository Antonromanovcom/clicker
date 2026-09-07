package dev.awake.mode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class MacOsTextEditActivityMode extends AbstractLifecycleMode {
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(8);
    private static final String FIND_DOCUMENT =
            "set targetDoc to missing value\n"
            + "tell application \"TextEdit\"\n"
            + "repeat with candidate in documents\n"
            + "try\n"
            + "if (path of candidate as text) is targetPath then set targetDoc to candidate\n"
            + "end try\n"
            + "end repeat\n"
            + "end tell\n"
            + "if targetDoc is missing value then error \"Awake temporary document is not open\"\n";

    private static final String READY_SCRIPT =
            "on run argv\nset targetPath to item 1 of argv\n" + FIND_DOCUMENT
            + "return \"ready\"\nend run";

    private static final String ACCESSIBILITY_SCRIPT =
            "tell application \"System Events\" to return UI elements enabled as text";

    private static final String PULSE_SCRIPT =
            "on run argv\n"
            + "set targetPath to item 1 of argv\n"
            + "set expectedCount to (item 2 of argv) as integer\n"
            + "set eraseAfter to (item 3 of argv) as integer\n"
            + FIND_DOCUMENT
            + "tell application \"TextEdit\"\n"
            + "set docName to name of targetDoc\n"
            + "set actualCount to count characters of (text of targetDoc)\n"
            + "if actualCount is not expectedCount then error \"Temporary document content changed unexpectedly\"\n"
            + "end tell\n"
            + "tell application \"System Events\"\n"
            + "set frontProcess to first application process whose frontmost is true\n"
            + "set previousBundle to bundle identifier of frontProcess\n"
            + "set previousName to name of frontProcess\n"
            + "end tell\n"
            + "tell application \"TextEdit\"\n"
            + "if previousName is \"TextEdit\" and name of front document is not docName then return \"SKIPPED\"\n"
            + "activate\nset index of window docName to 1\nend tell\n"
            + "delay 0.15\n"
            + "tell application \"System Events\"\n"
            + "if name of first application process whose frontmost is true is not \"TextEdit\" then error \"TextEdit did not receive focus\"\n"
            + "key code 124 using command down\nkeystroke \"x\"\nend tell\n"
            + "delay 0.1\n"
            + "set newCount to expectedCount + 1\n"
            + "tell application \"TextEdit\"\n"
            + "if (count characters of (text of targetDoc)) is not newCount then error \"Typed character was not confirmed\"\n"
            + "end tell\n"
            + "if eraseAfter > 0 and newCount is eraseAfter then\n"
            + "tell application \"System Events\"\nrepeat eraseAfter times\nkey code 51\nend repeat\nend tell\n"
            + "delay 0.1\n"
            + "tell application \"TextEdit\" to if (count characters of (text of targetDoc)) is not 0 then error \"Batch deletion was not confirmed\"\n"
            + "set newCount to 0\nend if\n"
            + "if previousBundle is not \"com.apple.TextEdit\" then do shell script \"open -b \" & quoted form of previousBundle\n"
            + "return newCount as text\nend run";

    private static final String CLOSE_SCRIPT =
            "on run argv\nset targetPath to item 1 of argv\n" + FIND_DOCUMENT
            + "tell application \"TextEdit\" to close targetDoc saving no\nreturn \"closed\"\nend run";

    private final Path requestedTarget;
    private final Long eraseAfter;
    private Path temporaryDocument;
    private int confirmedCharacters;

    MacOsTextEditActivityMode(Path requestedTarget, Long eraseAfter) {
        this.requestedTarget = requestedTarget;
        this.eraseAfter = eraseAfter;
    }

    @Override
    public String description() {
        String erase = eraseAfter == null ? "no batch deletion" : "erase each batch of " + eraseAfter;
        return "macOS TextEdit activity in an isolated temporary document (" + erase + ")";
    }

    @Override
    protected void onStart() throws ModeException {
        validateTarget();
        String accessibilityEnabled = runOsa(ACCESSIBILITY_SCRIPT);
        if (!"true".equalsIgnoreCase(accessibilityEnabled)) {
            throw new ModeException("macOS Accessibility permission is disabled. Enable it for the terminal "
                    + "or Java launcher in System Settings > Privacy & Security > Accessibility, then retry.");
        }
        try {
            temporaryDocument = Files.createTempFile("awake-activity-", ".txt").toRealPath();
            runCommand(List.of("/usr/bin/open", "-a", "TextEdit", temporaryDocument.toString()));
            long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            ModeException lastFailure = null;
            while (System.nanoTime() < deadline) {
                try {
                    runOsa(READY_SCRIPT, temporaryDocument.toString());
                    return;
                } catch (ModeException exception) {
                    lastFailure = exception;
                    Thread.sleep(100);
                }
            }
            throw new ModeException("TextEdit did not open the Awake temporary document in time.", lastFailure);
        } catch (IOException exception) {
            throw new ModeException("Could not create the isolated activity document.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ModeException("Interrupted while opening TextEdit.", exception);
        }
    }

    @Override
    public void pulse() throws ModeException {
        String result = runOsa(PULSE_SCRIPT,
                temporaryDocument.toString(),
                Integer.toString(confirmedCharacters),
                eraseAfter == null ? "0" : eraseAfter.toString());
        if ("SKIPPED".equals(result)) {
            return;
        }
        try {
            confirmedCharacters = Integer.parseInt(result);
        } catch (NumberFormatException exception) {
            throw new ModeException("TextEdit returned an unexpected activity result.", exception);
        }
    }

    @Override
    protected void onStop() throws ModeException {
        if (temporaryDocument == null) {
            return;
        }
        ModeException failure = null;
        try {
            runOsa(CLOSE_SCRIPT, temporaryDocument.toString());
            confirmedCharacters = 0;
        } catch (ModeException exception) {
            failure = exception;
        }
        try {
            Files.deleteIfExists(temporaryDocument);
        } catch (IOException exception) {
            ModeException deleteFailure = new ModeException("Could not delete the activity temporary file.", exception);
            if (failure == null) failure = deleteFailure; else failure.addSuppressed(deleteFailure);
        } finally {
            temporaryDocument = null;
        }
        if (failure != null) {
            throw failure;
        }
    }

    private void validateTarget() throws ModeException {
        Path fileName = requestedTarget.getFileName();
        if (Files.isDirectory(requestedTarget)
                && (fileName == null || !fileName.toString().equalsIgnoreCase("TextEdit.app"))) {
            throw new ModeException("On macOS, an application target must be TextEdit.app; a file path is also accepted.");
        }
    }

    private String runOsa(String script, String... arguments) throws ModeException {
        List<String> command = new ArrayList<>();
        command.add("/usr/bin/osascript");
        command.add("-e");
        command.add(script);
        command.addAll(List.of(arguments));
        return runCommand(command);
    }

    private String runCommand(List<String> command) throws ModeException {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(COMMAND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new ModeException("Timed out while controlling TextEdit.");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new ModeException("TextEdit automation failed: " + output);
            }
            return output;
        } catch (IOException exception) {
            throw new ModeException("Could not start macOS TextEdit automation.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ModeException("TextEdit automation was interrupted.", exception);
        }
    }
}
