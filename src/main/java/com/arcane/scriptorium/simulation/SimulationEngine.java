package com.arcane.scriptorium.simulation;

import com.arcane.scriptorium.domain.AccessRole;
import com.arcane.scriptorium.domain.Grimoire;
import com.arcane.scriptorium.domain.ProcessDescriptor;
import com.arcane.scriptorium.events.EventBus;
import com.arcane.scriptorium.events.EventType;
import com.arcane.scriptorium.events.SimulationEvent;
import com.arcane.scriptorium.synchronization.ArcaneSynchronizationCoordinator;
import com.arcane.scriptorium.synchronization.SynchronizationSnapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class SimulationEngine {
    private final List<Grimoire> grimoires;
    private final List<ArcaneSynchronizationCoordinator> coordinators;
    private final EventBus eventBus;
    private final List<ArcaneAgent> agents;
    private final List<Thread> threads;
    private final AtomicInteger manualIdGenerator = new AtomicInteger(5000);

    private SimulationEngine(
            List<Grimoire> grimoires,
            List<ArcaneSynchronizationCoordinator> coordinators,
            EventBus eventBus,
            List<ArcaneAgent> agents) {
        this.grimoires = grimoires;
        this.coordinators = coordinators;
        this.eventBus = eventBus;
        this.agents = new CopyOnWriteArrayList<>(agents);
        this.threads = new CopyOnWriteArrayList<>();
    }

    public static SimulationEngine defaultScenario(SimulationConfig config, EventBus eventBus) {
        List<Grimoire> grimoires = List.of(new Grimoire("Codex Umbrae"));
        List<ArcaneSynchronizationCoordinator> coordinators = List.of(
                new ArcaneSynchronizationCoordinator(config.maxCriticalReadersBeforeWriter(), eventBus));

        List<ProcessDescriptor> descriptors = List.of(
                new ProcessDescriptor(1, "Mago Harry", AccessRole.COMMON_READER),
                new ProcessDescriptor(2, "Maga Hermione", AccessRole.COMMON_READER),
                new ProcessDescriptor(3, "Mago Ron", AccessRole.COMMON_READER),
                new ProcessDescriptor(4, "Feiticeiro Voldemort", AccessRole.CRITICAL_READER),
                new ProcessDescriptor(5, "Feiticeiro Sauron", AccessRole.CRITICAL_READER),
                new ProcessDescriptor(6, "Anciao Gandalf", AccessRole.WRITER),
                new ProcessDescriptor(7, "Anciao Dumbledore", AccessRole.WRITER));

        List<ArcaneAgent> agents = descriptors.stream()
                .map(descriptor -> AgentFactory.create(descriptor, grimoires, coordinators, config, eventBus))
                .toList();

        return new SimulationEngine(grimoires, coordinators, eventBus, agents);
    }

    public static SimulationEngine elementalScenario(SimulationConfig config, EventBus eventBus) {
        List<Grimoire> grimoires = List.of(
                new Grimoire("Fogo"),
                new Grimoire("Agua"),
                new Grimoire("Terra"),
                new Grimoire("Ar"));

        List<ArcaneSynchronizationCoordinator> coordinators = List.of(
                new ArcaneSynchronizationCoordinator(config.maxCriticalReadersBeforeWriter(), eventBus),
                new ArcaneSynchronizationCoordinator(config.maxCriticalReadersBeforeWriter(), eventBus),
                new ArcaneSynchronizationCoordinator(config.maxCriticalReadersBeforeWriter(), eventBus),
                new ArcaneSynchronizationCoordinator(config.maxCriticalReadersBeforeWriter(), eventBus));

        List<ProcessDescriptor> descriptors = List.of(
                new ProcessDescriptor(1, "Mago Harry", AccessRole.COMMON_READER),
                new ProcessDescriptor(2, "Maga Hermione", AccessRole.COMMON_READER),
                new ProcessDescriptor(3, "Mago Ron", AccessRole.COMMON_READER),
                new ProcessDescriptor(4, "Feiticeiro Voldemort", AccessRole.CRITICAL_READER),
                new ProcessDescriptor(5, "Feiticeiro Sauron", AccessRole.CRITICAL_READER),
                new ProcessDescriptor(6, "Anciao Gandalf", AccessRole.WRITER),
                new ProcessDescriptor(7, "Anciao Dumbledore", AccessRole.WRITER));

        List<ArcaneAgent> agents = descriptors.stream()
                .map(descriptor -> AgentFactory.create(descriptor, grimoires, coordinators, config, eventBus))
                .toList();

        return new SimulationEngine(grimoires, coordinators, eventBus, agents);
    }

    public static SimulationEngine automaticScenario(SimulationConfig config, EventBus eventBus, int grimoireCount, 
                                                     int commonReadersCount, int criticalReadersCount, int writersCount) {
        List<Grimoire> grimoires = new ArrayList<>();
        List<ArcaneSynchronizationCoordinator> coordinators = new ArrayList<>();

        for (int i = 0; i < grimoireCount; i++) {
            String name;
            if (grimoireCount == 1) {
                name = "Codex Umbrae";
            } else if (grimoireCount == 4) {
                String[] elementalNames = {"Agua", "Terra", "Fogo", "Ar"};
                name = elementalNames[i];
            } else {
                name = "Grimorio " + (i + 1);
            }
            grimoires.add(new Grimoire(name));
            coordinators.add(new ArcaneSynchronizationCoordinator(config.maxCriticalReadersBeforeWriter(), eventBus));
        }

        List<ProcessDescriptor> descriptors = new ArrayList<>();
        int idCounter = 1;

        String[] commonNames = {"Mago Harry", "Maga Hermione", "Mago Ron"};
        for (int i = 0; i < commonReadersCount; i++) {
            descriptors.add(new ProcessDescriptor(idCounter++, commonNames[i % commonNames.length], AccessRole.COMMON_READER));
        }

        String[] criticalNames = {"Feiticeiro Voldemort", "Feiticeiro Sauron"};
        for (int i = 0; i < criticalReadersCount; i++) {
            descriptors.add(new ProcessDescriptor(idCounter++, criticalNames[i % criticalNames.length], AccessRole.CRITICAL_READER));
        }

        String[] writerNames = {"Anciao Gandalf", "Anciao Dumbledore"};
        for (int i = 0; i < writersCount; i++) {
            descriptors.add(new ProcessDescriptor(idCounter++, writerNames[i % writerNames.length], AccessRole.WRITER));
        }

        List<ArcaneAgent> agents = descriptors.stream()
                .map(descriptor -> AgentFactory.create(descriptor, grimoires, coordinators, config, eventBus))
                .toList();

        return new SimulationEngine(grimoires, coordinators, eventBus, agents);
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

    public void spawnManualCommonReader(Duration accessTime) {
        String[] names = {"Mago Harry", "Maga Hermione", "Mago Ron"};
        String chosen = names[java.util.concurrent.ThreadLocalRandom.current().nextInt(names.length)];
        spawnManualProcess(chosen, AccessRole.COMMON_READER, accessTime);
    }

    public void spawnManualCriticalReader(Duration accessTime) {
        String[] names = {"Feiticeiro Voldemort", "Feiticeiro Sauron"};
        String chosen = names[java.util.concurrent.ThreadLocalRandom.current().nextInt(names.length)];
        spawnManualProcess(chosen, AccessRole.CRITICAL_READER, accessTime);
    }

    public void spawnManualWriter(Duration accessTime) {
        String[] names = {"Anciao Gandalf", "Anciao Dumbledore"};
        String chosen = names[java.util.concurrent.ThreadLocalRandom.current().nextInt(names.length)];
        spawnManualProcess(chosen, AccessRole.WRITER, accessTime);
    }

    public void interruptAgent(int agentId) {
        for (int i = 0; i < agents.size(); i++) {
            if (agents.get(i).descriptor().id() == agentId) {
                threads.get(i).interrupt();
                break;
            }
        }
    }

    public void setStarvationPreventionEnabled(boolean preventStarvation) {
        for (ArcaneSynchronizationCoordinator coordinator : coordinators) {
            coordinator.setStarvationPreventionEnabled(preventStarvation);
        }
    }


    private void spawnManualProcess(String name, AccessRole role, Duration accessTime) {
        ProcessDescriptor descriptor = new ProcessDescriptor(manualIdGenerator.getAndIncrement(), name, role);
        SimulationConfig customConfig = SimulationConfig.manualInjectionConfig(accessTime);
        ArcaneAgent agent = AgentFactory.create(descriptor, grimoires, coordinators, customConfig, eventBus);
        
        agent.setOneShot(true);
        agents.add(agent);
        Thread thread = new Thread(agent, agent.metrics().process().label());
        threads.add(thread);
        thread.start();
    }


    public void pause() {
        publishSystem("Pausando a simulacao...");
        for (ArcaneAgent agent : agents) {
            agent.pause();
        }
    }

    public void resume() {
        publishSystem("Retomando a simulacao...");
        for (ArcaneAgent agent : agents) {
            agent.resume();
        }
    }

    public List<Grimoire> grimoires() {
        return grimoires;
    }
    
    public List<ArcaneAgent> agents() {
        return agents;
    }
    
    public SynchronizationSnapshot finalSnapshot() {
        return primaryCoordinator().snapshot();
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
                        metric.maxWaitMillis())));

        SynchronizationSnapshot snapshot = primaryCoordinator().snapshot();
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
                primaryCoordinator().snapshot()));
    }

    private ArcaneSynchronizationCoordinator primaryCoordinator() {
        return coordinators.get(0);
    }

    private void joinQuietly(Thread thread) {
        try {
            thread.join(2_000L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
