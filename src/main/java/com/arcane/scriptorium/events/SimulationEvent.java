package com.arcane.scriptorium.events;

import com.arcane.scriptorium.domain.ProcessDescriptor;
import com.arcane.scriptorium.domain.ProcessState;
import com.arcane.scriptorium.synchronization.SynchronizationSnapshot;

import java.time.Instant;

public record SimulationEvent(
        Instant timestamp,
        EventType type,
        ProcessDescriptor process,
        ProcessState state,
        String message,
        SynchronizationSnapshot snapshot
) {
    public static SimulationEvent now(
            EventType type,
            ProcessDescriptor process,
            ProcessState state,
            String message,
            SynchronizationSnapshot snapshot
    ) {
        return new SimulationEvent(Instant.now(), type, process, state, message, snapshot);
    }
}
