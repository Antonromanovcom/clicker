package dev.awake.mode;

import com.sun.jna.Library;
import com.sun.jna.Native;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

final class WindowsInhibitMode extends AbstractLifecycleMode {
    private static final int ES_CONTINUOUS = 0x80000000;
    private static final int ES_SYSTEM_REQUIRED = 0x00000001;
    private static final int ES_DISPLAY_REQUIRED = 0x00000002;
    private static final long TIMEOUT_SECONDS = 5;

    private final Kernel32 kernel32;
    private final boolean displayAwake;
    private CountDownLatch stopRequested;
    private Thread ownerThread;
    private AtomicReference<ModeException> failure;

    WindowsInhibitMode() {
        this(false);
    }

    WindowsInhibitMode(boolean displayAwake) {
        this(Native.load("kernel32", Kernel32.class), displayAwake);
    }

    WindowsInhibitMode(Kernel32 kernel32) {
        this(kernel32, false);
    }

    WindowsInhibitMode(Kernel32 kernel32, boolean displayAwake) {
        this.kernel32 = kernel32;
        this.displayAwake = displayAwake;
    }

    @Override
    public String description() {
        return displayAwake
                ? "Windows SetThreadExecutionState (system and display kept awake)"
                : "Windows SetThreadExecutionState (display sleep remains enabled)";
    }

    @Override
    protected void onStart() throws ModeException {
        CountDownLatch startupFinished = new CountDownLatch(1);
        stopRequested = new CountDownLatch(1);
        failure = new AtomicReference<>();
        ownerThread = new Thread(() -> runAssertion(startupFinished), "awake-windows-inhibit");
        ownerThread.start();
        try {
            if (!startupFinished.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                stopRequested.countDown();
                throw new ModeException("Timed out while activating the Windows power request.");
            }
        } catch (InterruptedException exception) {
            stopRequested.countDown();
            Thread.currentThread().interrupt();
            throw new ModeException("Interrupted while activating the Windows power request.", exception);
        }
        if (failure.get() != null) {
            throw failure.get();
        }
    }

    private void runAssertion(CountDownLatch startupFinished) {
        boolean assertionSet = false;
        try {
            int requirements = ES_CONTINUOUS | ES_SYSTEM_REQUIRED;
            if (displayAwake) requirements |= ES_DISPLAY_REQUIRED;
            assertionSet = kernel32.SetThreadExecutionState(requirements) != 0;
            if (!assertionSet) {
                failure.set(new ModeException("Windows rejected SetThreadExecutionState."));
                return;
            }
        } catch (Throwable throwable) {
            failure.set(new ModeException("Could not call the Windows power API.", throwable));
            return;
        } finally {
            startupFinished.countDown();
        }

        try {
            stopRequested.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            if (assertionSet && kernel32.SetThreadExecutionState(ES_CONTINUOUS) == 0) {
                failure.compareAndSet(null,
                        new ModeException("Windows did not release the power request cleanly."));
            }
        }
    }

    @Override
    protected void onStop() throws ModeException {
        if (stopRequested == null) {
            return;
        }
        stopRequested.countDown();
        try {
            ownerThread.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            if (ownerThread.isAlive()) {
                ownerThread.interrupt();
                throw new ModeException("Timed out while releasing the Windows power request.");
            }
        } catch (InterruptedException exception) {
            ownerThread.interrupt();
            Thread.currentThread().interrupt();
            throw new ModeException("Interrupted while releasing the Windows power request.", exception);
        }
        if (failure.get() != null) {
            throw failure.get();
        }
    }

    interface Kernel32 extends Library {
        int SetThreadExecutionState(int executionState);
    }
}
