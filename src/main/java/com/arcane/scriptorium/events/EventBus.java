package com.arcane.scriptorium.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventBus {
    private final List<SimulationObserver> observers = new CopyOnWriteArrayList<>();

    public void addObserver(SimulationObserver observer) {
        observers.add(observer);
    }

    public void publish(SimulationEvent event) {
        for (SimulationObserver observer : observers) {
            observer.onEvent(event);
        }
    }
}
