package dev.awake.cli;

public final class ParseResult {
    public enum Action { RUN, HELP, VERSION }

    private final Action action;
    private final CliOptions options;

    private ParseResult(Action action, CliOptions options) {
        this.action = action;
        this.options = options;
    }

    static ParseResult run(CliOptions options) {
        return new ParseResult(Action.RUN, options);
    }

    static ParseResult action(Action action) {
        return new ParseResult(action, null);
    }

    public Action getAction() {
        return action;
    }

    public CliOptions getOptions() {
        return options;
    }
}

