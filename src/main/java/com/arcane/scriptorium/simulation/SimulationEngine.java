package com.arcane.scriptorium.simulation;

import com.arcane.scriptorium.domain.AccessRole;
import com.arcane.scriptorium.domain.Grimoire;
import com.arcane.scriptorium.domain.ProcessDescriptor;
import com.arcane.scriptorium.events.EventBus;
import com.arcane.scriptorium.events.EventType;
import com.arcane.scriptorium.events.SimulationEvent;
import com.arcane.scriptorium.synchronization.ArcaneSynchronizationCoordinator;
import com.arcane.scriptorium.synchronization.SynchronizationSnapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SimulationEngine {
    private final Grimoire grimoire;
    private final ArcaneSynchronizationCoordinator coordinator;
    private final EventBus eventBus;
    private final List<ArcaneAgent> agents;
    private final List<Thread> threads;

    private SimulationEngine(
            Grimoire grimoire,
            ArcaneSynchronizationCoordinator coordinator,
            EventBus eventBus,
            List<ArcaneAgent> agents
    ) {
        this.grimoire = grimoire;
        this.coordinator = coordinator;
        this.eventBus = eventBus;
        this.agents = agents;
        this.threads = new ArrayList<>();
    }

    public static SimulationEngine defaultScenario(SimulationConfig config, EventBus eventBus) {
        Grimoire grimoire = new Grimoire("Codex Umbrae");
        ArcaneSynchronizationCoordinator coordinator = new ArcaneSynchronizationCoordinator(
                config.maxCriticalReadersBeforeWriter(),
                eventBus
        );

        List<ProcessDescriptor> descriptors = List.of(
                new ProcessDescriptor(1, "Ariadne", AccessRole.COMMON_READER),
                new ProcessDescriptor(2, "Boreal", AccessRole.COMMON_READER),
                new ProcessDescriptor(3, "Calisto", AccessRole.COMMON_READER),
                new ProcessDescriptor(4, "Damaris", AccessRole.CRITICAL_READER),
                new ProcessDescriptor(5, "Eldrin", AccessRole.CRITICAL_READER),
                new ProcessDescriptor(6, "Fausto", AccessRole.WRITER),
                new ProcessDescriptor(7, "Galen", AccessRole.WRITER)
        );

        List<ArcaneAgent> agents = descriptors.stream()
                .map(descriptor -> AgentFactory.create(descriptor, grimoire, coordinator, config, eventBus))
                .toList();

        return new SimulationEngine(grimoire, coordinator, eventBus, agents);
    }

    public void start() {
        publishSystem("Iniciando simulacao da Biblioteca Arcana.");
        for (ArcaneAgent agent : agents) {
            Thread thread = new Thread(agent, agent.metrics().process().label());
            thread.start();
            threads.add(thread);
        }
    }

    public void stop() {
        publishSystem("Encerrando simulacao e interrompendo agentes.");
        for (Thread thread : threads) {
            thread.interrupt();
        }
        for (Thread thread : threads) {
            joinQuietly(thread);
        }
        publishSystem("Todos os agentes foram encerrados.");
    }

    public Grimoire grimoire() {
        return grimoire;
    }

    public String metricsReport() {
        StringBuilder report = new StringBuilder();
        report.append("Relatorio final de metricas\n");
        report.append("=".repeat(76)).append('\n');
        report.append("%-28s %-16s %8s %12s %12s %12s%n"
                .formatted("Processo", "Tipo", "Acessos", "EsperaTotal", "EsperaMedia", "EsperaMax"));

        agents.stream()
                .map(ArcaneAgent::metrics)
                .sorted(Comparator.comparingInt(metric -> metric.process().id()))
                .forEach(metric -> report.append("%-28s %-16s %8d %12d %12d %12d%n".formatted(
                        metric.process().shortName(),
                        metric.process().role().displayName(),
                        metric.accesses(),
                        metric.totalWaitMillis(),
                        metric.averageWaitMillis(),
                        metric.maxWaitMillis()
                )));

        SynchronizationSnapshot snapshot = coordinator.snapshot();
        report.append("=".repeat(76)).append('\n');
        report.append("Estado final: ").append(snapshot.compact()).append('\n');
        return report.toString();
    }

    private void publishSystem(String message) {
        eventBus.publish(new SimulationEvent(
                Instant.now(),
                EventType.SYSTEM,
                null,
                null,
                message,
                coordinator.snapshot()
        ));
    }

    private void joinQuietly(Thread thread) {
        try {
            thread.join(2_000L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
