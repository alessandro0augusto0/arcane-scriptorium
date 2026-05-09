package com.arcane.scriptorium.ui.console;

import com.arcane.scriptorium.domain.AccessRole;
import com.arcane.scriptorium.domain.ProcessDescriptor;
import com.arcane.scriptorium.events.EventType;
import com.arcane.scriptorium.events.SimulationEvent;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class ConsoleEventRenderer implements com.arcane.scriptorium.events.SimulationObserver {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    private final boolean ansiEnabled;
    private final Object outputLock;

    public ConsoleEventRenderer(boolean ansiEnabled) {
        this.ansiEnabled = ansiEnabled;
        this.outputLock = new Object();
    }

    @Override
    public void onEvent(SimulationEvent event) {
        synchronized (outputLock) {
            System.out.println(format(event));
        }
    }

    private String format(SimulationEvent event) {
        String time = Ansi.paint(ansiEnabled, Ansi.GRAY, TIME_FORMATTER.format(event.timestamp()));
        String type = Ansi.paint(ansiEnabled, colorFor(event.type(), event.process()), "%-8s".formatted(event.type()));
        String process = event.process() == null ? "[SISTEMA]" : event.process().label();
        String state = event.state() == null ? "-" : event.state().name();
        String snapshot = event.snapshot() == null ? "" : Ansi.paint(ansiEnabled, Ansi.DIM, " | " + event.snapshot().compact());

        return "%s %s %-26s %-9s %s%s".formatted(
                time,
                type,
                process,
                state,
                event.message(),
                snapshot
        );
    }

    private String colorFor(EventType type, ProcessDescriptor process) {
        if (type == EventType.BLOCKED) {
            return Ansi.YELLOW;
        }
        if (type == EventType.ENTERED) {
            return Ansi.GREEN;
        }
        if (type == EventType.EXITED) {
            return Ansi.CYAN;
        }
        if (type == EventType.POLICY) {
            return Ansi.MAGENTA;
        }
        if (process == null) {
            return Ansi.GRAY;
        }
        AccessRole role = process.role();
        return switch (role) {
            case COMMON_READER -> Ansi.BLUE;
            case CRITICAL_READER -> Ansi.MAGENTA;
            case WRITER -> Ansi.RED;
        };
    }
}
