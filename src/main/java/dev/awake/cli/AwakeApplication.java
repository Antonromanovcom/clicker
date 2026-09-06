package dev.awake.cli;

import java.io.PrintStream;

/** Entry point for the Awake command-line application. */
public final class AwakeApplication {
    public static final String VERSION = "0.2.0-SNAPSHOT";

    private AwakeApplication() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != ExitCode.SUCCESS) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            ParseResult result = new CliParser().parse(args);
            if (result.getAction() == ParseResult.Action.HELP) {
                printHelp(out);
                return ExitCode.SUCCESS;
            }
            if (result.getAction() == ParseResult.Action.VERSION) {
                out.println("Awake " + VERSION);
                return ExitCode.SUCCESS;
            }

            CliOptions options = result.getOptions();
            out.println("Awake " + VERSION);
            out.println("Mode: " + options.getMode().cliValue());
            out.println("Configuration accepted. This mode is not implemented yet.");
            return ExitCode.SUCCESS;
        } catch (CliException exception) {
            err.println("Error: " + exception.getMessage());
            err.println("Run with --help for usage.");
            return ExitCode.INVALID_ARGUMENTS;
        }
    }

    private static void printHelp(PrintStream out) {
        out.println("Awake " + VERSION);
        out.println("Usage:");
        out.println("  java -jar awake.jar --mode <inhibit|activity> (--hours N | --until HH:mm) [options]");
        out.println();
        out.println("Options:");
        out.println("  --mode <mode>       Required mode: inhibit or activity");
        out.println("  --hours <N>         Run for a positive whole number of hours");
        out.println("  --from <HH:mm>      Delay start; valid only together with --until");
        out.println("  --until <HH:mm>     End at local time in 24-hour format");
        out.println("  --target <path>     Required for activity; forbidden for inhibit");
        out.println("  --interval <sec>    Activity interval in seconds (default: 60)");
        out.println("  --help              Show this help and exit");
        out.println("  --version           Show version and exit");
        out.println();
        out.println("The inhibit and activity behaviors are not implemented yet.");
    }
}

