package com.arcane.scriptorium.synchronization;

import com.arcane.scriptorium.domain.ProcessDescriptor;

public final class AccessPermit implements AutoCloseable {
    private final ArcaneSynchronizationCoordinator coordinator;
    private final ProcessDescriptor process;
    private final long waitedMillis;
    private boolean closed;

    AccessPermit(ArcaneSynchronizationCoordinator coordinator, ProcessDescriptor process, long waitedMillis) {
        this.coordinator = coordinator;
        this.process = process;
        this.waitedMillis = waitedMillis;
    }

    public long waitedMillis() {
        return waitedMillis;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            coordinator.release(process);
        }
    }
}
