package dev.awake.mode;

public interface WakefulnessMode {
    String description();

    void start() throws ModeException;

    void stop() throws ModeException;

    boolean isRunning();
}

