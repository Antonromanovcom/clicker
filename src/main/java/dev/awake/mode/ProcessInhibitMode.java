package dev.awake.mode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

abstract class ProcessInhibitMode extends AbstractLifecycleMode {
    private static final long STARTUP_CHECK_MILLIS = 150;
    private static final long STOP_TIMEOUT_MILLIS = 2_000;

    private final String backendName;
    private final List<String> command;
    private Process process;

    ProcessInhibitMode(String backendName, List<String> command) {
        this.backendName = backendName;
        this.command = List.copyOf(command);
    }

    @Override
    public String description() {
        return "system inhibit via " + backendName + " (display sleep remains enabled)";
    }

    @Override
    protected void onStart() throws ModeException {
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (process.waitFor(STARTUP_CHECK_MILLIS, TimeUnit.MILLISECONDS)) {
                String details = new String(
                        process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                process = null;
                throw new ModeException(backendName + " exited during startup"
                        + (details.isEmpty() ? "." : ": " + details));
            }
        } catch (IOException exception) {
            process = null;
            throw new ModeException("Cannot start " + backendName
                    + ". Is it installed and executable?", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            process = null;
            throw new ModeException("Interrupted while starting " + backendName + ".", exception);
        }
    }

    @Override
    protected void onStop() throws ModeException {
        Process activeProcess = process;
        process = null;
        if (activeProcess == null || !activeProcess.isAlive()) {
            return;
        }
        activeProcess.descendants().forEach(ProcessHandle::destroy);
        activeProcess.destroy();
        try {
            if (!activeProcess.waitFor(STOP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                activeProcess.descendants().forEach(ProcessHandle::destroyForcibly);
                activeProcess.destroyForcibly();
                if (!activeProcess.waitFor(STOP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                    throw new ModeException("Could not stop " + backendName + ".");
                }
            }
        } catch (InterruptedException exception) {
            activeProcess.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new ModeException("Interrupted while stopping " + backendName + ".", exception);
        }
    }
}
