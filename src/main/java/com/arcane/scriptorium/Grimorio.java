package com.arcane.scriptorium;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Grimorio {
    private static final int LIMITE_LEITORES_CONSECUTIVOS = 5;
    private final Semaphore mutexLeitura;
    private final Semaphore mutexEscrita;
    private final Semaphore catraca;
    private final AtomicInteger leitoresAtivos;
    private final AtomicInteger leitoresConsecutivos;

    public Grimorio() {
        this.mutexLeitura = new Semaphore(1);
        this.mutexEscrita = new Semaphore(1);
        this.catraca = new Semaphore(1);
        this.leitoresAtivos = new AtomicInteger(0);
        this.leitoresConsecutivos = new AtomicInteger(0);
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

    public int incrementarLeitoresConsecutivos() {
        return leitoresConsecutivos.incrementAndGet();
    }

    public void zerarLeitoresConsecutivos() {
        leitoresConsecutivos.set(0);
    }

    public boolean isLimiteAtingido() {
        return leitoresConsecutivos.get() >= LIMITE_LEITORES_CONSECUTIVOS;
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
