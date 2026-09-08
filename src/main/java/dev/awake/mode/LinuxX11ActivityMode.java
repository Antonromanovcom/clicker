package dev.awake.mode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class LinuxX11ActivityMode extends AbstractLifecycleMode {
    private static final Duration TIMEOUT = Duration.ofSeconds(8);
    private final Path editorExecutable;
    private final Long eraseAfter;
    private Path temporaryFile;
    private Process editor;
    private String windowId;
    private int confirmedCharacters;

    LinuxX11ActivityMode(Path editorExecutable, Long eraseAfter) {
        this.editorExecutable = editorExecutable;
        this.eraseAfter = eraseAfter;
    }

    @Override
    public String description() {
        return "Linux X11 activity via xdotool in an isolated temporary file ("
                + (eraseAfter == null ? "no batch deletion" : "erase each batch of " + eraseAfter) + ")";
    }

    @Override
    protected void onStart() throws ModeException {
        if (!Files.isRegularFile(editorExecutable) || !Files.isExecutable(editorExecutable)) {
            throw new ModeException("Linux activity target must be an executable editor path.");
        }
        run(List.of("xdotool", "version"));
        try {
            temporaryFile = Files.createTempFile("awake-activity-", ".txt");
            editor = new ProcessBuilder(editorExecutable.toString(), temporaryFile.toString()).start();
            long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
            while (System.nanoTime() < deadline && editor.isAlive()) {
                String found = runAllowFailure(List.of(
                        "xdotool", "search", "--onlyvisible", "--pid", Long.toString(editor.pid()),
                        "--name", temporaryFile.getFileName().toString()));
                if (!found.isBlank() && !found.startsWith("EXIT:")) {
                    windowId = found.lines().findFirst().orElseThrow();
                    return;
                }
                Thread.sleep(150);
            }
            throw new ModeException("No X11 window for the launched editor and temporary file was found.");
        } catch (IOException exception) {
            throw new ModeException("Could not create the Linux activity workspace.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ModeException("Interrupted while starting the Linux editor.", exception);
        }
    }

    @Override
    public void pulse() throws ModeException {
        if (editor == null || !editor.isAlive()) throw new ModeException("The target Linux editor was closed.");
        String pid = run(List.of("xdotool", "getwindowpid", windowId)).trim();
        if (!pid.equals(Long.toString(editor.pid()))) throw new ModeException("Linux target window PID changed.");
        String previous = run(List.of("xdotool", "getactivewindow")).trim();
        try {
            run(List.of("xdotool", "windowactivate", "--sync", windowId));
            String active = run(List.of("xdotool", "getactivewindow")).trim();
            if (!active.equals(windowId)) {
                throw new ModeException("Linux target window could not be activated safely.");
            }
            run(List.of("xdotool", "key", "--window", windowId, "ctrl+End"));
            run(List.of("xdotool", "type", "--window", windowId, "--delay", "0", "x"));
            run(List.of("xdotool", "key", "--window", windowId, "ctrl+s"));
            waitForLength(confirmedCharacters + 1);
            confirmedCharacters++;
            if (eraseAfter != null && confirmedCharacters == eraseAfter) {
                run(List.of("xdotool", "key", "--window", windowId,
                        "--repeat", Integer.toString(confirmedCharacters), "--delay", "0", "BackSpace"));
                run(List.of("xdotool", "key", "--window", windowId, "ctrl+s"));
                waitForLength(0);
                confirmedCharacters = 0;
            }
        } finally {
            if (!previous.isBlank()) runAllowFailure(List.of("xdotool", "windowactivate", "--sync", previous));
        }
    }

    @Override
    protected void onStop() throws ModeException {
        if (editor != null && editor.isAlive()) {
            editor.destroy();
            try {
                if (!editor.waitFor(3, TimeUnit.SECONDS)) editor.destroyForcibly();
            } catch (InterruptedException exception) {
                editor.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
        try {
            if (temporaryFile != null) Files.deleteIfExists(temporaryFile);
        } catch (IOException exception) {
            throw new ModeException("Could not delete the Linux activity temporary file.", exception);
        } finally {
            editor = null;
            temporaryFile = null;
            windowId = null;
            confirmedCharacters = 0;
        }
    }

    private void waitForLength(int expected) throws ModeException {
        long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (System.nanoTime() < deadline) {
            try {
                String text = Files.readString(temporaryFile);
                if (text.length() == expected && (expected == 0 || text.endsWith("x"))) return;
                Thread.sleep(50);
            } catch (IOException exception) {
                throw new ModeException("Could not verify the Linux activity temporary file.", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ModeException("Interrupted while verifying Linux activity.", exception);
            }
        }
        throw new ModeException("Linux editor did not save the expected temporary content.");
    }

    private String run(List<String> command) throws ModeException {
        String output = runAllowFailure(command);
        if (output.startsWith("EXIT:")) throw new ModeException("Linux activity command failed: " + output);
        return output;
    }

    private String runAllowFailure(List<String> command) throws ModeException {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new ModeException("Linux activity command timed out.");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return process.exitValue() == 0 ? output : "EXIT:" + process.exitValue() + ":" + output;
        } catch (IOException exception) {
            throw new ModeException("Could not execute Linux activity automation; is xdotool installed?", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ModeException("Linux activity automation was interrupted.", exception);
        }
    }
}
