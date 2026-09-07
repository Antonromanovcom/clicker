package dev.awake.mode;

import java.nio.file.Path;

final class ActivityPlaceholderMode extends AbstractPlaceholderMode {
    private final Platform platform;
    private final Path target;

    ActivityPlaceholderMode(Platform platform, Path target) {
        this.platform = platform;
        this.target = target;
    }

    @Override
    public String description() {
        return "activity placeholder for " + platform.name().toLowerCase()
                + " targeting " + target + "; no input is generated";
    }
}

