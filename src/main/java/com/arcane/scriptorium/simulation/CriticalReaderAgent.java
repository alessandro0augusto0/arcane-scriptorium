package com.arcane.scriptorium.simulation;

import com.arcane.scriptorium.domain.Grimoire;
import com.arcane.scriptorium.domain.ProcessDescriptor;
import com.arcane.scriptorium.domain.ProcessState;
import com.arcane.scriptorium.events.EventBus;
import com.arcane.scriptorium.events.EventType;
import com.arcane.scriptorium.synchronization.ArcaneSynchronizationCoordinator;
import com.arcane.scriptorium.utils.RandomDuration;

import java.time.Duration;
import java.util.List;

public final class CriticalReaderAgent extends ArcaneAgent {
    public CriticalReaderAgent(
            ProcessDescriptor descriptor,
            List<Grimoire> grimoires,
            List<ArcaneSynchronizationCoordinator> coordinators,
            SimulationConfig config,
            EventBus eventBus) {
        super(descriptor, grimoires, coordinators, config, eventBus);
    }

    @Override
    protected Duration activityDuration() {
        return RandomDuration.between(config().minCriticalRead(), config().maxCriticalRead());
    }

    @Override
    protected void enterCriticalRegion(Grimoire grimoire, ArcaneSynchronizationCoordinator coordinator) {
        publish(coordinator, EventType.STATE, ProcessState.READING, grimoire.read(descriptor()));
    }
}
