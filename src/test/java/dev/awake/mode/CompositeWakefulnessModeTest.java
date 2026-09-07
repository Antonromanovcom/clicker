package dev.awake.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompositeWakefulnessModeTest {
    @Test
    void startsInOrderAndStopsInReverseOrder() throws Exception {
        List<String> events = new ArrayList<>();
        CompositeWakefulnessMode mode = new CompositeWakefulnessMode(
                new RecordingMode("inhibit", events, false),
                new RecordingMode("activity", events, false));

        mode.start();
        mode.stop();

        assertEquals(List.of("start inhibit", "start activity", "stop activity", "stop inhibit"), events);
    }

    @Test
    void rollsBackAlreadyStartedModesWhenLaterStartFails() {
        List<String> events = new ArrayList<>();
        CompositeWakefulnessMode mode = new CompositeWakefulnessMode(
                new RecordingMode("inhibit", events, false),
                new RecordingMode("activity", events, true));

        assertThrows(ModeException.class, mode::start);
        assertFalse(mode.isRunning());
        assertEquals(List.of("start inhibit", "start activity", "stop inhibit"), events);
    }

    private static final class RecordingMode extends AbstractLifecycleMode {
        private final String name;
        private final List<String> events;
        private final boolean failStart;

        private RecordingMode(String name, List<String> events, boolean failStart) {
            this.name = name;
            this.events = events;
            this.failStart = failStart;
        }

        public String description() { return name; }
        protected void onStart() throws ModeException {
            events.add("start " + name);
            if (failStart) throw new ModeException("failed " + name);
        }
        protected void onStop() { events.add("stop " + name); }
    }
}

