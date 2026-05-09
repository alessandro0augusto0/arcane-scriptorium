package com.arcane.scriptorium.simulation;

import com.arcane.scriptorium.domain.Grimoire;
import com.arcane.scriptorium.domain.ProcessDescriptor;
import com.arcane.scriptorium.domain.ProcessState;
import com.arcane.scriptorium.events.EventBus;
import com.arcane.scriptorium.events.EventType;
import com.arcane.scriptorium.events.SimulationEvent;
import com.arcane.scriptorium.synchronization.AccessPermit;
import com.arcane.scriptorium.synchronization.ArcaneSynchronizationCoordinator;
import com.arcane.scriptorium.utils.RandomDuration;

import java.time.Duration;

public abstract class ArcaneAgent implements Runnable {
    private final ProcessDescriptor descriptor;
    private final Grimoire grimoire;
    private final ArcaneSynchronizationCoordinator coordinator;
    private final SimulationConfig config;
    private final EventBus eventBus;
    private final ProcessMetrics metrics;

    protected ArcaneAgent(
            ProcessDescriptor descriptor,
            Grimoire grimoire,
            ArcaneSynchronizationCoordinator coordinator,
            SimulationConfig config,
            EventBus eventBus
    ) {
        this.descriptor = descriptor;
        this.grimoire = grimoire;
        this.coordinator = coordinator;
        this.config = config;
        this.eventBus = eventBus;
        this.metrics = new ProcessMetrics(descriptor);
    }

    @Override
    public final void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                rest();
                waitForAccess();
                try (AccessPermit permit = coordinator.acquire(descriptor)) {
                    metrics.registerAccess(permit.waitedMillis());
                    enterCriticalRegion();
                    Thread.sleep(activityDuration().toMillis());
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            publish(EventType.STATE, ProcessState.STOPPED, "Processo encerrado.");
        }
    }

    public ProcessMetrics metrics() {
        return metrics.snapshot();
    }

    protected final ProcessDescriptor descriptor() {
        return descriptor;
    }

    protected final Grimoire grimoire() {
        return grimoire;
    }

    protected final SimulationConfig config() {
        return config;
    }

    protected final void publish(EventType type, ProcessState state, String message) {
        eventBus.publish(SimulationEvent.now(type, descriptor, state, message, coordinator.snapshot()));
    }

    protected abstract Duration activityDuration();

    protected abstract void enterCriticalRegion();

    private void rest() throws InterruptedException {
        publish(EventType.STATE, ProcessState.RESTING, "Descansando antes da proxima tentativa.");
        Thread.sleep(RandomDuration.between(config.minRest(), config.maxRest()).toMillis());
    }

    private void waitForAccess() {
        publish(EventType.WAITING, ProcessState.WAITING, "Solicitou acesso ao grimorio.");
    }
}
