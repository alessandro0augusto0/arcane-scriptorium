package com.arcane.scriptorium.simulation;

import java.time.Duration;

public record SimulationConfig(
        Duration duration,
        int maxCriticalReadersBeforeWriter,
        Duration minRest,
        Duration maxRest,
        Duration minRead,
        Duration maxRead,
        Duration minCriticalRead,
        Duration maxCriticalRead,
        Duration minWrite,
        Duration maxWrite
) {
    public static SimulationConfig defaultConfig() {
        return new SimulationConfig(
                Duration.ofSeconds(15),
                5,
                Duration.ofMillis(250),
                Duration.ofMillis(850),
                Duration.ofMillis(500),
                Duration.ofMillis(1_000),
                Duration.ofMillis(350),
                Duration.ofMillis(750),
                Duration.ofMillis(700),
                Duration.ofMillis(1_150)
        );
    }

    public SimulationConfig withDuration(Duration newDuration) {
        return new SimulationConfig(
                newDuration,
                maxCriticalReadersBeforeWriter,
                minRest,
                maxRest,
                minRead,
                maxRead,
                minCriticalRead,
                maxCriticalRead,
                minWrite,
                maxWrite
        );
    }

    public SimulationConfig withMaxCriticalReadersBeforeWriter(int newLimit) {
        return new SimulationConfig(
                duration,
                Math.max(0, newLimit),
                minRest,
                maxRest,
                minRead,
                maxRead,
                minCriticalRead,
                maxCriticalRead,
                minWrite,
                maxWrite
        );
    }
}
