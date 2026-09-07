package dev.awake.mode;

import java.util.Locale;
import java.util.function.Supplier;

public final class PlatformDetector {
    private final Supplier<String> osName;

    public PlatformDetector() {
        this(() -> System.getProperty("os.name", ""));
    }

    PlatformDetector(Supplier<String> osName) {
        this.osName = osName;
    }

    public Platform detect() {
        String value = osName.get().toLowerCase(Locale.ROOT);
        if (value.contains("mac") || value.contains("darwin")) {
            return Platform.MACOS;
        }
        if (value.contains("win")) {
            return Platform.WINDOWS;
        }
        if (value.contains("linux")) {
            return Platform.LINUX;
        }
        return Platform.UNSUPPORTED;
    }
}

