package dev.awake.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AbstractLifecycleModeTest {
    @Test
    void startAndStopAreIdempotent() throws Exception {
        RecordingMode mode = new RecordingMode();

        mode.start();
        mode.start();
        assertTrue(mode.isRunning());
        mode.stop();
        mode.stop();

        assertFalse(mode.isRunning());
        assertEquals(1, mode.starts);
        assertEquals(1, mode.stops);
        assertThrows(ModeException.class, mode::start);
    }

    private static final class RecordingMode extends AbstractLifecycleMode {
        private int starts;
        private int stops;

        public String description() { return "test"; }
        protected void onStart() { starts++; }
        protected void onStop() { stops++; }
    }
}

