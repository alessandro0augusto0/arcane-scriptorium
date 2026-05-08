package com.arcane.scriptorium;

public class MagoEscritor extends Mago {
    public MagoEscritor(int id, String nome, Grimorio grimorio) {
        super(id, nome, grimorio);
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            setEstadoAtual(EstadoMago.DORMINDO);
            log("📜 " + getNome() + " #" + getIdMago() + " descansando.");
            dormir(700);

            setEstadoAtual(EstadoMago.AGUARDANDO_ACESSO);
            log("📜 " + getNome() + " #" + getIdMago() + " solicitou acesso para escrita.");
            long inicioEspera = System.currentTimeMillis();
            grimorio.down(grimorio.getCatraca());
            grimorio.down(grimorio.getMutexEscrita());
            grimorio.zerarLeitoresConsecutivos();

            long fimEspera = System.currentTimeMillis();
            registrarAcesso(fimEspera - inicioEspera);

            setEstadoAtual(EstadoMago.ESCREVENDO);
            log("📜 " + getNome() + " #" + getIdMago() + " trancou o grimorio de forma exclusiva.");
            dormir(1000);

            grimorio.up(grimorio.getMutexEscrita());
            grimorio.up(grimorio.getCatraca());
        }
    }
}
