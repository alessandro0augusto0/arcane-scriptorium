package com.arcane.scriptorium;

public class MagoLeitor extends Mago {
    public MagoLeitor(int id, String nome, Grimorio grimorio) {
        super(id, nome, grimorio);
    }

    public MagoLeitor(int id, String nome, Grimorio grimorio, boolean cicloUnico) {
        super(id, nome, grimorio, cicloUnico);
    }

    @Override
    protected void executarCiclo() {
        setEstadoAtual(EstadoMago.DORMINDO);
        log("📘 " + getNome() + " #" + getIdMago() + " descansando.");
        dormir(600);

        setEstadoAtual(EstadoMago.AGUARDANDO_ACESSO);
        log("📘 " + getNome() + " #" + getIdMago() + " solicitou acesso para leitura.");
        dormir(2000);
        long inicioEspera = System.currentTimeMillis();
        grimorio.down(grimorio.getCatraca());
        grimorio.up(grimorio.getCatraca());

        grimorio.down(grimorio.getMutexLeitura());
        int leitores = grimorio.incrementarLeitores();
        if (leitores == 1) {
            grimorio.down(grimorio.getMutexEscrita());
        }
        grimorio.up(grimorio.getMutexLeitura());

        long fimEspera = System.currentTimeMillis();
        registrarAcesso(fimEspera - inicioEspera);

        setEstadoAtual(EstadoMago.LENDO);
        log("📘 " + getNome() + " #" + getIdMago() + " iniciou a leitura do grimorio.");
        dormir(3800);

        grimorio.down(grimorio.getMutexLeitura());
        int restantes = grimorio.decrementarLeitores();
        if (restantes == 0) {
            grimorio.up(grimorio.getMutexEscrita());
        }
        grimorio.up(grimorio.getMutexLeitura());

        setEstadoAtual(EstadoMago.DORMINDO);
        log("📘 " + getNome() + " #" + getIdMago() + " concluiu o ciclo.");
    }
}
