package dev.awake.mode;

final class InhibitPlaceholderMode extends AbstractPlaceholderMode {
    private final Platform platform;

    InhibitPlaceholderMode(Platform platform) {
        this.platform = platform;
    }

    @Override
    public String description() {
        return "inhibit placeholder for " + platform.name().toLowerCase()
                + "; no system sleep API is active";
    }
}

