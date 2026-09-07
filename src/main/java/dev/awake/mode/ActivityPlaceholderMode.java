package dev.awake.mode;

import java.nio.file.Path;

final class ActivityPlaceholderMode extends AbstractPlaceholderMode {
    private final Platform platform;
    private final Path target;
    private final long intervalSeconds;
    private final Long eraseAfter;
    private final boolean dryRun;

    ActivityPlaceholderMode(Platform platform, Path target, long intervalSeconds,
                            Long eraseAfter, boolean dryRun) {
        this.platform = platform;
        this.target = target;
        this.intervalSeconds = intervalSeconds;
        this.eraseAfter = eraseAfter;
        this.dryRun = dryRun;
    }

    @Override
    public String description() {
        String erasePolicy = eraseAfter == null
                ? "typed characters would be retained"
                : "each batch of " + eraseAfter + " typed characters would be erased";
        return (dryRun ? "activity dry-run" : "activity placeholder")
                + " for " + platform.name().toLowerCase()
                + " using an isolated target every " + intervalSeconds + "s; "
                + erasePolicy + "; no input is generated yet";
    }
}
