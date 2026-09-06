package dev.awake.scheduling;

import dev.awake.cli.CliOptions;
import java.io.PrintStream;

@FunctionalInterface
public interface ExecutionScheduler {
    int execute(CliOptions options, TimeWindow window, PrintStream out);
}

