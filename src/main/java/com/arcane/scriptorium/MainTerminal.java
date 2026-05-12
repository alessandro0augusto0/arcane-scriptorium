package com.arcane.scriptorium;

import com.arcane.scriptorium.events.EventBus;
import com.arcane.scriptorium.simulation.SimulationConfig;
import com.arcane.scriptorium.simulation.SimulationEngine;
import com.arcane.scriptorium.ui.console.Ansi;
import com.arcane.scriptorium.ui.console.ConsoleEventRenderer;

import java.time.Duration;

public final class MainTerminal {
    private static final long DEFAULT_DURATION_MS = 15_000L;

    private MainTerminal() {
    }

    public static void main(String[] args) {
        long durationMillis = parseDuration(args);

        EventBus eventBus = new EventBus();
        eventBus.addObserver(new ConsoleEventRenderer(Ansi.isEnabled()));

        SimulationConfig config = SimulationConfig.defaultConfig()
                .withDuration(Duration.ofMillis(durationMillis))
                .withMaxCriticalReadersBeforeWriter(parseCriticalLimit(args));

        SimulationEngine engine = SimulationEngine.defaultScenario(config, eventBus);
        engine.start();
        sleep(durationMillis);
        engine.stop();

        System.out.println();
        System.out.println(engine.metricsReport());

        System.out.println();
        System.out.println(Ansi.paint(Ansi.isEnabled(), Ansi.YELLOW, 
            "DICA: Esta execução ocorreu com a prevenção de inanição (starvation) ATIVADA."));
        System.out.println(Ansi.paint(Ansi.isEnabled(), Ansi.YELLOW, 
            "Para visualizar o problema de inanição acontecendo na prática e interagir com o algoritmo,"));
        System.out.println(Ansi.paint(Ansi.isEnabled(), Ansi.YELLOW, 
            "abra a Interface Gráfica (GUI) da Biblioteca Arcana."));
    }

    private static long parseDuration(String[] args) {
        if (args.length == 0) {
            return DEFAULT_DURATION_MS;
        }

        try {
            return Math.max(1_000L, Long.parseLong(args[0]));
        } catch (NumberFormatException ignored) {
            return DEFAULT_DURATION_MS;
        }
    }

    private static int parseCriticalLimit(String[] args) {
        if (args.length < 2) {
            return SimulationConfig.defaultConfig().maxCriticalReadersBeforeWriter();
        }

        try {
            return Math.max(0, Integer.parseInt(args[1]));
        } catch (NumberFormatException ignored) {
            return SimulationConfig.defaultConfig().maxCriticalReadersBeforeWriter();
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
