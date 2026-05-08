package com.arcane.scriptorium;

public class MagoLeitor extends Mago {
    public MagoLeitor(int id, String nome, Grimorio grimorio) {
        super(id, nome, grimorio);
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            setEstadoAtual(EstadoMago.DORMINDO);
            log("📘 " + getNome() + " #" + getIdMago() + " descansando.");
            dormir(600);

            setEstadoAtual(EstadoMago.AGUARDANDO_ACESSO);
            log("📘 " + getNome() + " #" + getIdMago() + " solicitou acesso para leitura.");
            long inicioEspera = System.currentTimeMillis();
            grimorio.down(grimorio.getCatraca());
            grimorio.up(grimorio.getCatraca());

            grimorio.down(grimorio.getMutexLeitura());
            int leitores = grimorio.incrementarLeitores();
            if (leitores == 1) {
                grimorio.down(grimorio.getMutexEscrita());
            }
            grimorio.up(grimorio.getMutexLeitura());
            grimorio.incrementarLeitoresConsecutivos();

            long fimEspera = System.currentTimeMillis();
            registrarAcesso(fimEspera - inicioEspera);

            setEstadoAtual(EstadoMago.LENDO);
            log("📘 " + getNome() + " #" + getIdMago() + " iniciou a leitura do grimorio.");
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
