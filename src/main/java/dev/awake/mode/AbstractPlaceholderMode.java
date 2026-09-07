package dev.awake.mode;

abstract class AbstractPlaceholderMode implements WakefulnessMode {
    private State state = State.NEW;

    @Override
    public synchronized void start() throws ModeException {
        if (state == State.STOPPED) {
            throw new ModeException("A stopped mode instance cannot be started again.");
        }
        state = State.RUNNING;
    }

    @Override
    public synchronized void stop() {
        if (state == State.STOPPED) {
            return;
        }
        state = State.STOPPED;
    }

    @Override
    public synchronized boolean isRunning() {
        return state == State.RUNNING;
    }

    private enum State {
        NEW,
        RUNNING,
        STOPPED
    }
}

