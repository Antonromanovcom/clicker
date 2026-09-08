package dev.awake.cli;

import java.io.PrintStream;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import dev.awake.scheduling.AwakeScheduler;
import dev.awake.scheduling.ExecutionScheduler;
import dev.awake.scheduling.TimeWindow;
import dev.awake.scheduling.TimeWindowResolver;
import dev.awake.mode.ModeException;
import dev.awake.mode.ModeFactory;
import dev.awake.mode.ModeProvider;
import dev.awake.mode.PlatformDetector;
import dev.awake.mode.WakefulnessMode;

/** Entry point for the Awake command-line application. */
public final class AwakeApplication {
    public static final String VERSION = "0.7.1-SNAPSHOT";
    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss VV");

    private AwakeApplication() {
    }

    public static void main(String[] args) {
        Clock clock = Clock.systemDefaultZone();
        int exitCode = run(args, System.out, System.err, clock, new AwakeScheduler(clock),
                new ModeFactory(new PlatformDetector()));
        if (exitCode != ExitCode.SUCCESS) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        Clock clock = Clock.systemDefaultZone();
        return run(args, out, err, clock, new AwakeScheduler(clock),
                new ModeFactory(new PlatformDetector()));
    }

    static int run(String[] args, PrintStream out, PrintStream err,
                   Clock clock, ExecutionScheduler scheduler, ModeProvider modeProvider) {
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
            TimeWindow window = new TimeWindowResolver(clock).resolve(options);
            WakefulnessMode mode = modeProvider.create(options);
            if (options.getLogLevel() != LogLevel.QUIET) {
                out.println("Awake " + VERSION);
            }
            if (options.isDryRun()) {
                if (options.getLogLevel() != LogLevel.QUIET) {
                    printDryRun(window, mode, out);
                }
                return ExitCode.SUCCESS;
            }
            return scheduler.execute(options, window, mode, out);
        } catch (CliException exception) {
            err.println("Error: " + exception.getMessage());
            err.println("Run with --help for usage.");
            return ExitCode.INVALID_ARGUMENTS;
        } catch (ModeException exception) {
            err.println("Platform error: " + exception.getMessage());
            return ExitCode.PLATFORM_ERROR;
        }
    }

    private static void printDryRun(TimeWindow window, WakefulnessMode mode, PrintStream out) {
        out.println("Dry-run: no scheduler, system inhibit, file access, or input will be started.");
        out.println("Starts: " + DISPLAY_TIME.format(window.getStart()));
        out.println("Ends:   " + DISPLAY_TIME.format(window.getEnd()));
        out.println("Plan:   " + mode.description() + ".");
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
        out.println("  --no-inhibit        Activity: do not also block system sleep");
        out.println("  --erase-after <N>   Activity: erase each batch of N typed characters (max: 100)");
        out.println("  --dry-run           Activity: validate and report actions without system effects");
        out.println("  --log-level <level> Logging: quiet, normal (default), or verbose");
        out.println("  --help              Show this help and exit");
        out.println("  --version           Show version and exit");
        out.println();
        out.println("Activity supports Windows Notepad, macOS TextEdit, and Linux X11 editors via xdotool.");
    }
}
