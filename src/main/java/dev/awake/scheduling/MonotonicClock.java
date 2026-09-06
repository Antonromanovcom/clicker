package dev.awake.scheduling;

@FunctionalInterface
interface MonotonicClock {
    long nanoTime();
}

