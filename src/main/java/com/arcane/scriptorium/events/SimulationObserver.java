package com.arcane.scriptorium.events;

@FunctionalInterface
public interface SimulationObserver {
    void onEvent(SimulationEvent event);
}
