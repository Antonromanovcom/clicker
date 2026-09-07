package dev.awake.mode;

public interface WakefulnessMode {
    String description();

    void start() throws ModeException;

    /** Perform one scheduled unit of activity. Inhibit-only modes use the default no-op. */
    default void pulse() throws ModeException {
    }

    void stop() throws ModeException;

    boolean isRunning();
}
