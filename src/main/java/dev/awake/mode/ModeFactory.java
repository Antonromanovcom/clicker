package dev.awake.mode;

import dev.awake.cli.AwakeMode;
import dev.awake.cli.CliOptions;

public final class ModeFactory implements ModeProvider {
    private final PlatformDetector platformDetector;

    public ModeFactory(PlatformDetector platformDetector) {
        this.platformDetector = platformDetector;
    }

    @Override
    public WakefulnessMode create(CliOptions options) throws ModeException {
        Platform platform = platformDetector.detect();
        if (platform == Platform.UNSUPPORTED) {
            throw new ModeException("Unsupported operating system: "
                    + System.getProperty("os.name", "unknown"));
        }
        if (options.getMode() == AwakeMode.INHIBIT) {
            return new InhibitPlaceholderMode(platform);
        }
        return new ActivityPlaceholderMode(platform, options.getTarget());
    }
}

