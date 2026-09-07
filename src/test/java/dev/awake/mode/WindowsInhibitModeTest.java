package dev.awake.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WindowsInhibitModeTest {
    @Test
    void setsAndClearsExecutionStateOnSameOwnerThread() throws Exception {
        RecordingKernel32 api = new RecordingKernel32(1);
        WindowsInhibitMode mode = new WindowsInhibitMode(api);

        mode.start();
        assertTrue(mode.isRunning());
        mode.stop();

        assertFalse(mode.isRunning());
        assertEquals(List.of(0x80000001, 0x80000000), api.flags);
        assertEquals(1, api.threadIds.stream().distinct().count());
    }

    @Test
    void reportsRejectedPowerRequest() {
        WindowsInhibitMode mode = new WindowsInhibitMode(new RecordingKernel32(0));

        assertThrows(ModeException.class, mode::start);
    }

    private static final class RecordingKernel32 implements WindowsInhibitMode.Kernel32 {
        private final int result;
        private final List<Integer> flags = new ArrayList<>();
        private final List<Long> threadIds = new ArrayList<>();

        private RecordingKernel32(int result) {
            this.result = result;
        }

        @Override
        public synchronized int SetThreadExecutionState(int executionState) {
            flags.add(executionState);
            threadIds.add(Thread.currentThread().getId());
            return result;
        }
    }
}

