package dev.awake.mode;

abstract class AbstractLifecycleMode implements WakefulnessMode {
    private State state = State.NEW;

    @Override
    public final synchronized void start() throws ModeException {
        if (state == State.RUNNING) {
            return;
        }
        if (state == State.STOPPED) {
            throw new ModeException("A stopped mode instance cannot be started again.");
        }
        onStart();
        state = State.RUNNING;
    }

    @Override
    public final synchronized void stop() throws ModeException {
        if (state == State.STOPPED) {
            return;
        }
        try {
            onStop();
        } finally {
            state = State.STOPPED;
        }
    }

    @Override
    public final synchronized boolean isRunning() {
        return state == State.RUNNING;
    }

    protected abstract void onStart() throws ModeException;
    protected abstract void onStop() throws ModeException;

    private enum State { NEW, RUNNING, STOPPED }
}
