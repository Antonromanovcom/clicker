package dev.awake.mode;

import dev.awake.cli.CliOptions;

@FunctionalInterface
public interface ModeProvider {
    WakefulnessMode create(CliOptions options) throws ModeException;
}

