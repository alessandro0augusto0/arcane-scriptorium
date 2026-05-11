package com.arcane.scriptorium.utils;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomDuration {
    private RandomDuration() {
    }

    public static Duration between(Duration min, Duration max) {
        long minMillis = min.toMillis();
        long maxMillis = max.toMillis();
        if (maxMillis <= minMillis) {
            return min;
        }
        return Duration.ofMillis(ThreadLocalRandom.current().nextLong(minMillis, maxMillis + 1));
    }
}
