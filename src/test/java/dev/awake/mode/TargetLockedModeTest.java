package dev.awake.mode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TargetLockedModeTest {
    @Test
    void preventsConcurrentUseAndReleasesLockOnStop() throws Exception {
        Path target = Path.of("/tmp/awake-lock-test-target");
        TargetLockedMode first = new TargetLockedMode(target, new Placeholder());
        TargetLockedMode second = new TargetLockedMode(target, new Placeholder());

        first.start();
        assertThrows(ModeException.class, second::start);
        first.stop();

        TargetLockedMode afterRelease = new TargetLockedMode(target, new Placeholder());
        assertDoesNotThrow(afterRelease::start);
        afterRelease.stop();
    }

    private static final class Placeholder extends AbstractPlaceholderMode {
        public String description() { return "test placeholder"; }
    }
}

