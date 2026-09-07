package dev.awake.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlatformDetectorTest {
    @Test
    void detectsSupportedOperatingSystems() {
        assertEquals(Platform.MACOS, detector("Mac OS X").detect());
        assertEquals(Platform.WINDOWS, detector("Windows 11").detect());
        assertEquals(Platform.LINUX, detector("Linux").detect());
    }

    @Test
    void marksUnknownOperatingSystemAsUnsupported() {
        assertEquals(Platform.UNSUPPORTED, detector("Plan 9").detect());
    }

    private PlatformDetector detector(String name) {
        return new PlatformDetector(() -> name);
    }
}

