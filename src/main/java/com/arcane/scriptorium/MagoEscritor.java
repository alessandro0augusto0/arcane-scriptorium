package com.arcane.scriptorium;

public class MagoEscritor extends Mago {
    public MagoEscritor(int id, String nome, Grimorio grimorio) {
        super(id, nome, grimorio);
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            setEstadoAtual(EstadoMago.DORMINDO);
            dormir(700);

            setEstadoAtual(EstadoMago.AGUARDANDO_ACESSO);
            grimorio.down(grimorio.getCatraca());
            grimorio.down(grimorio.getMutexEscrita());

            setEstadoAtual(EstadoMago.ESCREVENDO);
            dormir(1000);

            grimorio.up(grimorio.getMutexEscrita());
            grimorio.up(grimorio.getCatraca());
        }
    }
}
