package dev.awake.cli;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class CliParser {
    private static final Set<String> VALUE_OPTIONS = Set.of(
            "--mode", "--hours", "--from", "--until", "--target", "--interval");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

    public ParseResult parse(String[] args) throws CliException {
        if (args.length == 1 && "--help".equals(args[0])) {
            return ParseResult.action(ParseResult.Action.HELP);
        }
        if (args.length == 1 && "--version".equals(args[0])) {
            return ParseResult.action(ParseResult.Action.VERSION);
        }
        if (args.length == 0) {
            throw new CliException("No arguments supplied.");
        }

        Map<String, String> values = collectValues(args);
        AwakeMode mode = requireMode(values);
        Long hours = parsePositiveLong(values.get("--hours"), "--hours");
        LocalTime from = parseTime(values.get("--from"), "--from");
        LocalTime until = parseTime(values.get("--until"), "--until");
        Path target = parsePath(values.get("--target"));
        Long configuredInterval = parsePositiveLong(values.get("--interval"), "--interval");
        long interval = configuredInterval == null ? 60 : configuredInterval;

        validate(mode, hours, from, until, target, values.containsKey("--interval"));
        return ParseResult.run(new CliOptions(mode, hours, from, until, target, interval));
    }

    private Map<String, String> collectValues(String[] args) throws CliException {
        Map<String, String> values = new HashMap<>();
        for (int index = 0; index < args.length; index++) {
            String name = args[index];
            if ("--help".equals(name) || "--version".equals(name)) {
                throw new CliException(name + " must be used on its own.");
            }
            if (!VALUE_OPTIONS.contains(name)) {
                throw new CliException("Unknown option '" + name + "'.");
            }
            if (values.containsKey(name)) {
                throw new CliException("Option " + name + " was supplied more than once.");
            }
            if (++index >= args.length || args[index].startsWith("--")) {
                throw new CliException("Option " + name + " requires a value.");
            }
            values.put(name, args[index]);
        }
        return values;
    }

    private AwakeMode requireMode(Map<String, String> values) throws CliException {
        String value = values.get("--mode");
        if (value == null) {
            throw new CliException("Required option --mode is missing.");
        }
        return AwakeMode.fromCliValue(value);
    }

    private Long parsePositiveLong(String value, String name) throws CliException {
        if (value == null) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) {
                throw new CliException(name + " must be greater than zero.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new CliException(name + " must be a positive whole number.");
        }
    }

    private LocalTime parseTime(String value, String name) throws CliException {
        if (value == null) {
            return null;
        }
        try {
            return LocalTime.parse(value, TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new CliException(name + " must use 24-hour HH:mm format.");
        }
    }

    private Path parsePath(String value) throws CliException {
        if (value == null) {
            return null;
        }
        try {
            return Path.of(value);
        } catch (InvalidPathException exception) {
            throw new CliException("--target is not a valid path.");
        }
    }

    private void validate(AwakeMode mode, Long hours, LocalTime from, LocalTime until,
                          Path target, boolean intervalSupplied) throws CliException {
        if ((hours == null) == (until == null)) {
            throw new CliException("Specify exactly one of --hours or --until.");
        }
        if (from != null && until == null) {
            throw new CliException("--from can only be used together with --until.");
        }
        if (mode == AwakeMode.ACTIVITY && target == null) {
            throw new CliException("Activity mode requires --target.");
        }
        if (mode == AwakeMode.INHIBIT && target != null) {
            throw new CliException("--target is only valid in activity mode.");
        }
        if (mode == AwakeMode.INHIBIT && intervalSupplied) {
            throw new CliException("--interval is only valid in activity mode.");
        }
    }
}

