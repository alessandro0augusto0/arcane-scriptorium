package com.arcane.scriptorium;

public class MagoLeitorCritico extends Mago {
    public MagoLeitorCritico(int id, String nome, Grimorio grimorio) {
        super(id, nome, grimorio);
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            setEstadoAtual(EstadoMago.DORMINDO);
            dormir(500);

            setEstadoAtual(EstadoMago.AGUARDANDO_ACESSO);
            long inicioEspera = System.currentTimeMillis();

            grimorio.down(grimorio.getMutexLeitura());
            int leitores = grimorio.incrementarLeitores();
            if (leitores == 1) {
                grimorio.down(grimorio.getMutexEscrita());
            }
            grimorio.up(grimorio.getMutexLeitura());

            long fimEspera = System.currentTimeMillis();
            registrarAcesso(fimEspera - inicioEspera);

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
