package dev.awake.scheduling;

import java.time.Duration;

@FunctionalInterface
interface Sleeper {
    void sleep(Duration duration) throws InterruptedException;
}

