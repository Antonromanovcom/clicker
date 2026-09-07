package dev.awake.mode;

import java.util.List;

final class CompositeWakefulnessMode extends AbstractLifecycleMode {
    private final List<WakefulnessMode> modes;

    CompositeWakefulnessMode(WakefulnessMode... modes) {
        this.modes = List.of(modes);
    }

    @Override
    public String description() {
        return modes.stream()
                .map(WakefulnessMode::description)
                .reduce((left, right) -> left + "; " + right)
                .orElse("empty composite mode");
    }

    @Override
    protected void onStart() throws ModeException {
        int started = 0;
        try {
            for (WakefulnessMode mode : modes) {
                mode.start();
                started++;
            }
        } catch (ModeException exception) {
            for (int index = started - 1; index >= 0; index--) {
                try {
                    modes.get(index).stop();
                } catch (ModeException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }
            throw exception;
        }
    }

    @Override
    protected void onStop() throws ModeException {
        ModeException failure = null;
        for (int index = modes.size() - 1; index >= 0; index--) {
            try {
                modes.get(index).stop();
            } catch (ModeException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    @Override
    public void pulse() throws ModeException {
        for (WakefulnessMode mode : modes) {
            mode.pulse();
        }
    }
}
