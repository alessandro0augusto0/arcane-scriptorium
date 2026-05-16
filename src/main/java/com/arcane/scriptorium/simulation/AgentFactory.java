package com.arcane.scriptorium.simulation;

import com.arcane.scriptorium.domain.AccessRole;
import com.arcane.scriptorium.domain.Grimoire;
import com.arcane.scriptorium.domain.ProcessDescriptor;
import com.arcane.scriptorium.events.EventBus;
import com.arcane.scriptorium.synchronization.ArcaneSynchronizationCoordinator;

import java.util.List;

public final class AgentFactory {
    private AgentFactory() {
    }

    public static ArcaneAgent create(
            ProcessDescriptor descriptor,
            List<Grimoire> grimoires,
            List<ArcaneSynchronizationCoordinator> coordinators,
            SimulationConfig config,
            EventBus eventBus) {
        AccessRole role = descriptor.role();
        return switch (role) {
            case COMMON_READER -> new CommonReaderAgent(descriptor, grimoires, coordinators, config, eventBus);
            case CRITICAL_READER -> new CriticalReaderAgent(descriptor, grimoires, coordinators, config, eventBus);
            case WRITER -> new WriterAgent(descriptor, grimoires, coordinators, config, eventBus);
        };
    }
}
