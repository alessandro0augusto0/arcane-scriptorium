package com.arcane.scriptorium;

public class MagoLeitor extends Mago {
    public MagoLeitor(int id, String nome, Grimorio grimorio) {
        super(id, nome, grimorio);
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            setEstadoAtual(EstadoMago.DORMINDO);
            dormir(600);

            setEstadoAtual(EstadoMago.AGUARDANDO_ACESSO);
            grimorio.down(grimorio.getCatraca());
            grimorio.up(grimorio.getCatraca());

            grimorio.down(grimorio.getMutexLeitura());
            int leitores = grimorio.incrementarLeitores();
            if (leitores == 1) {
                grimorio.down(grimorio.getMutexEscrita());
            }
            grimorio.up(grimorio.getMutexLeitura());

            setEstadoAtual(EstadoMago.LENDO);
            dormir(800);

            grimorio.down(grimorio.getMutexLeitura());
            int restantes = grimorio.decrementarLeitores();
            if (restantes == 0) {
                grimorio.up(grimorio.getMutexEscrita());
            }
            grimorio.up(grimorio.getMutexLeitura());
        }
    }
}
