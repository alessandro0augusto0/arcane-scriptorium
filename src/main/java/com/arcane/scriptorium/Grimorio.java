package com.arcane.scriptorium;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Grimorio {
    private final Semaphore mutexLeitura;
    private final Semaphore mutexEscrita;
    private final Semaphore catraca;
    private final AtomicInteger leitoresAtivos;

    public Grimorio() {
        this.mutexLeitura = new Semaphore(1);
        this.mutexEscrita = new Semaphore(1);
        this.catraca = new Semaphore(1);
        this.leitoresAtivos = new AtomicInteger(0);
    }

    public Semaphore getMutexLeitura() {
        return mutexLeitura;
    }

    public Semaphore getMutexEscrita() {
        return mutexEscrita;
    }

    public Semaphore getCatraca() {
        return catraca;
    }

    public int incrementarLeitores() {
        return leitoresAtivos.incrementAndGet();
    }

    public int decrementarLeitores() {
        return leitoresAtivos.decrementAndGet();
    }

    public int getLeitoresAtivos() {
        return leitoresAtivos.get();
    }

    public void down(Semaphore s) {
        try {
            s.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void up(Semaphore s) {
        s.release();
    }
}
