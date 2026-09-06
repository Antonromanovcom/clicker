package dev.awake.cli;

public enum AwakeMode {
    INHIBIT,
    ACTIVITY;

    static AwakeMode fromCliValue(String value) throws CliException {
        for (AwakeMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new CliException("Unknown mode '" + value + "'. Expected inhibit or activity.");
    }

    public String cliValue() {
        return name().toLowerCase();
    }
}

