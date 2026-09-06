package dev.awake.cli;

/** Stable process exit codes exposed by the CLI. */
public final class ExitCode {
    public static final int SUCCESS = 0;
    public static final int INVALID_ARGUMENTS = 2;
    public static final int PLATFORM_ERROR = 3;
    public static final int INTERRUPTED = 130;

    private ExitCode() {
    }
}

