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
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public abstract class ArcaneAgent implements Runnable {
    private final ProcessDescriptor descriptor;
    private final List<Grimoire> grimoires;
    private final List<ArcaneSynchronizationCoordinator> coordinators;
    private final SimulationConfig config;
    private final EventBus eventBus;
    private final ProcessMetrics metrics;

    protected ArcaneAgent(
            ProcessDescriptor descriptor,
            List<Grimoire> grimoires,
            List<ArcaneSynchronizationCoordinator> coordinators,
            SimulationConfig config,
            EventBus eventBus) {
        this.descriptor = descriptor;
        this.grimoires = grimoires;
        this.coordinators = coordinators;
        this.config = config;
        this.eventBus = eventBus;
        this.metrics = new ProcessMetrics(descriptor);
    }

    @Override
    public final void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                rest();
                int targetIndex = ThreadLocalRandom.current().nextInt(grimoires.size());
                ArcaneSynchronizationCoordinator targetCoordinator = coordinators.get(targetIndex);
                Grimoire targetGrimoire = grimoires.get(targetIndex);
                waitForAccess(targetCoordinator);
                try (AccessPermit permit = targetCoordinator.acquire(descriptor)) {
                    metrics.registerAccess(permit.waitedMillis());
                    enterCriticalRegion(targetGrimoire, targetCoordinator);
                    Thread.sleep(activityDuration().toMillis());
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            publish(primaryCoordinator(), EventType.STATE, ProcessState.STOPPED, "Processo encerrado.");
        }
    }

    public ProcessMetrics metrics() {
        return metrics.snapshot();
    }

    protected final ProcessDescriptor descriptor() {
        return descriptor;
    }

    protected final SimulationConfig config() {
        return config;
    }

    protected final void publish(
            ArcaneSynchronizationCoordinator coordinator,
            EventType type,
            ProcessState state,
            String message) {
        eventBus.publish(SimulationEvent.now(type, descriptor, state, message, coordinator.snapshot()));
    }

    protected abstract Duration activityDuration();

    protected abstract void enterCriticalRegion(
            Grimoire grimoire,
            ArcaneSynchronizationCoordinator coordinator);

    private void rest() throws InterruptedException {
        publish(primaryCoordinator(), EventType.STATE, ProcessState.RESTING,
                "Descansando antes da proxima tentativa.");
        Thread.sleep(RandomDuration.between(config.minRest(), config.maxRest()).toMillis());
    }

    private void waitForAccess(ArcaneSynchronizationCoordinator coordinator) {
        publish(coordinator, EventType.WAITING, ProcessState.WAITING, "Solicitou acesso ao grimorio.");
    }

    private ArcaneSynchronizationCoordinator primaryCoordinator() {
        return coordinators.get(0);
    }
}
