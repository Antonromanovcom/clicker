package dev.awake.cli;

public enum LogLevel {
    QUIET,
    NORMAL,
    VERBOSE;

    static LogLevel fromCliValue(String value) throws CliException {
        for (LogLevel level : values()) {
            if (level.name().equalsIgnoreCase(value)) {
                return level;
            }
        }
        throw new CliException("Unknown log level '" + value
                + "'. Expected quiet, normal or verbose.");
    }
}
