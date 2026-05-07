package com.arcane.scriptorium;

import java.util.concurrent.Semaphore;

public class Grimorio {
    private final Semaphore mutexLeitura;
    private final Semaphore mutexEscrita;
    private final Semaphore catraca;
    private int leitoresAtivos;

    public Grimorio() {
        this.mutexLeitura = new Semaphore(1);
        this.mutexEscrita = new Semaphore(1);
        this.catraca = new Semaphore(1);
        this.leitoresAtivos = 0;
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
        leitoresAtivos += 1;
        return leitoresAtivos;
    }

    public int decrementarLeitores() {
        leitoresAtivos -= 1;
        return leitoresAtivos;
    }

    public int getLeitoresAtivos() {
        return leitoresAtivos;
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
