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
            return createInhibitMode(platform);
        }
        WakefulnessMode activity = options.isDryRun() || platform != Platform.MACOS
                ? new ActivityPlaceholderMode(platform, options.getTarget(), options.getIntervalSeconds(),
                        options.getEraseAfter(), options.isDryRun())
                : new MacOsTextEditActivityMode(options.getTarget(), options.getEraseAfter());
        if (!options.isDryRun()) {
            activity = new TargetLockedMode(options.getTarget(), activity);
        }
        if (options.isDryRun() || options.isInhibitDisabled()) {
            return activity;
        }
        return new CompositeWakefulnessMode(createInhibitMode(platform), activity);
    }

    private WakefulnessMode createInhibitMode(Platform platform) throws ModeException {
        switch (platform) {
            case MACOS:
                return new MacOsInhibitMode();
            case WINDOWS:
                return new WindowsInhibitMode();
            case LINUX:
                return new LinuxInhibitMode();
            default:
                throw new ModeException("Unsupported operating system: "
                        + System.getProperty("os.name", "unknown"));
        }
    }
}
