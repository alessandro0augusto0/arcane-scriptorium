package com.arcane.scriptorium;

public class MagoEscritor extends Mago {
    public MagoEscritor(int id, String nome, Grimorio grimorio) {
        super(id, nome, grimorio);
    }

    public MagoEscritor(int id, String nome, Grimorio grimorio, boolean cicloUnico) {
        super(id, nome, grimorio, cicloUnico);
    }

    @Override
    protected void executarCiclo() {
        setEstadoAtual(EstadoMago.DORMINDO);
        log("📜 " + getNome() + " #" + getIdMago() + " descansando.");
        dormir(700);

        setEstadoAtual(EstadoMago.AGUARDANDO_ACESSO);
        log("📜 " + getNome() + " #" + getIdMago() + " solicitou acesso para escrita.");
        dormir(2000);
        long inicioEspera = System.currentTimeMillis();
        grimorio.down(grimorio.getCatraca());
        grimorio.down(grimorio.getMutexEscrita());

        long fimEspera = System.currentTimeMillis();
        registrarAcesso(fimEspera - inicioEspera);

        setEstadoAtual(EstadoMago.ESCREVENDO);
        log("📜 " + getNome() + " #" + getIdMago() + " trancou o grimorio de forma exclusiva.");
        dormir(4200);

        grimorio.up(grimorio.getMutexEscrita());
        grimorio.up(grimorio.getCatraca());

        setEstadoAtual(EstadoMago.DORMINDO);
        log("📜 " + getNome() + " #" + getIdMago() + " concluiu o ciclo.");
    }
}
