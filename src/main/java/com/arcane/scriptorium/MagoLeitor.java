package com.arcane.scriptorium;

public class MagoLeitor extends Mago {
    public MagoLeitor(int id, String nome, Grimorio grimorio) {
        super(id, nome, grimorio);
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            setEstadoAtual(EstadoMago.DORMINDO);
            // ciclo de vida: dormir, solicitar leitura, ler, finalizar
            setEstadoAtual(EstadoMago.AGUARDANDO_ACESSO);
            grimorio.solicitarLeitura(this);
            setEstadoAtual(EstadoMago.LENDO);
            grimorio.finalizarLeitura(this);
        }
    }
}
