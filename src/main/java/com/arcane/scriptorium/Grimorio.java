package com.arcane.scriptorium;

import java.util.concurrent.Semaphore;

public class Grimorio {
    private final Semaphore mutexLeitura;
    private final Semaphore mutexEscrita;
    private final Semaphore catraca;

    public Grimorio() {
        this.mutexLeitura = new Semaphore(1, true);
        this.mutexEscrita = new Semaphore(1, true);
        this.catraca = new Semaphore(1, true);
    }

    public void solicitarLeitura(MagoLeitor leitor) {
        // logica de concorrencia sera implementada no Commit 3
    }

    public void finalizarLeitura(MagoLeitor leitor) {
        // logica de concorrencia sera implementada no Commit 3
    }

    public void solicitarEscrita(MagoEscritor escritor) {
        // logica de concorrencia sera implementada no Commit 3
    }

    public void finalizarEscrita(MagoEscritor escritor) {
        // logica de concorrencia sera implementada no Commit 3
    }

    public void down(Semaphore s) throws InterruptedException {
        s.acquire();
    }

    public void up(Semaphore s) {
        s.release();
    }
}
