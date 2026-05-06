package com.arcane.scriptorium;

public class MagoEscritor extends Mago {
    public MagoEscritor(int id, String nome, Grimorio grimorio) {
        super(id, nome, grimorio);
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            setEstadoAtual(EstadoMago.DORMINDO);
            // ciclo de vida: dormir, solicitar escrita, escrever, finalizar
            setEstadoAtual(EstadoMago.AGUARDANDO_ACESSO);
            grimorio.solicitarEscrita(this);
            setEstadoAtual(EstadoMago.ESCREVENDO);
            grimorio.finalizarEscrita(this);
        }
    }
}
