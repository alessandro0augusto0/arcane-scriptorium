package com.arcane.scriptorium;

public class MagoLeitorCritico extends Mago {
    public MagoLeitorCritico(int id, String nome, Grimorio grimorio) {
        super(id, nome, grimorio);
    }

    public MagoLeitorCritico(int id, String nome, Grimorio grimorio, boolean cicloUnico) {
        super(id, nome, grimorio, cicloUnico);
    }

    @Override
    protected void executarCiclo() {
        setEstadoAtual(EstadoMago.DORMINDO);
        log("⚡ " + getNome() + " #" + getIdMago() + " descansando.");
        dormir(500);

        setEstadoAtual(EstadoMago.AGUARDANDO_ACESSO);
        log("⚡ " + getNome() + " #" + getIdMago() + " ignorou a catraca para pesquisa critica.");
        dormir(2000);
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
        log("⚡ " + getNome() + " #" + getIdMago() + " iniciou a leitura critica do grimorio.");
        dormir(3600);

        grimorio.down(grimorio.getMutexLeitura());
        int restantes = grimorio.decrementarLeitores();
        if (restantes == 0) {
            grimorio.up(grimorio.getMutexEscrita());
        }
        grimorio.up(grimorio.getMutexLeitura());

        setEstadoAtual(EstadoMago.DORMINDO);
        log("⚡ " + getNome() + " #" + getIdMago() + " concluiu o ciclo.");
    }
}
