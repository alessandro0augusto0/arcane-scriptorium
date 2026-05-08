package com.arcane.scriptorium;

public class MagoLeitorCritico extends Mago {
    public MagoLeitorCritico(int id, String nome, Grimorio grimorio) {
        super(id, nome, grimorio);
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            setEstadoAtual(EstadoMago.DORMINDO);
            log("⚡ " + getNome() + " #" + getIdMago() + " descansando.");
            dormir(500);

            setEstadoAtual(EstadoMago.AGUARDANDO_ACESSO);
            long inicioEspera = System.currentTimeMillis();

            if (grimorio.isLimiteAtingido()) {
                log("⚡ [LIMITE ATINGIDO] Mago Critico " + getNome() + " #" + getIdMago()
                        + " foi rebaixado para a fila da catraca.");
                grimorio.down(grimorio.getCatraca());
                grimorio.up(grimorio.getCatraca());
            } else {
                log("⚡ " + getNome() + " #" + getIdMago() + " ignorou a catraca para pesquisa critica.");
            }

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
            log("⚡ " + getNome() + " #" + getIdMago() + " iniciou a leitura critica do grimorio.");
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
