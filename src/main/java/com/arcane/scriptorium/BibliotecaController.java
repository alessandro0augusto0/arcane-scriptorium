package com.arcane.scriptorium;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BibliotecaController {
    private final Grimorio grimorio;
    private final List<Mago> magos;
    private Consumer<Mago> magoListener;

    public BibliotecaController() {
        this.grimorio = new Grimorio();
        this.magos = new ArrayList<>();
    }

    public void adicionarLeitor(int id, String nome) {
        MagoLeitor mago = new MagoLeitor(id, nome, grimorio);
        registrarListener(mago);
        magos.add(mago);
    }

    public void adicionarEscritor(int id, String nome) {
        MagoEscritor mago = new MagoEscritor(id, nome, grimorio);
        registrarListener(mago);
        magos.add(mago);
    }

    public void adicionarLeitorCritico(int id, String nome) {
        MagoLeitorCritico mago = new MagoLeitorCritico(id, nome, grimorio);
        registrarListener(mago);
        magos.add(mago);
    }

    public List<Mago> getMagos() {
        return new ArrayList<>(magos);
    }

    public Grimorio getGrimorio() {
        return grimorio;
    }

    public void setMagoListener(Consumer<Mago> magoListener) {
        this.magoListener = magoListener;
        for (Mago mago : magos) {
            registrarListener(mago);
        }
    }

    public void iniciarSimulacao() {
        for (Mago mago : magos) {
            mago.start();
        }
    }

    public String pararSimulacao() {
        for (Mago mago : magos) {
            mago.interrupt();
        }

        StringBuilder relatorio = new StringBuilder();
        relatorio.append("Relatorio final de metricas:");
        System.out.println(relatorio);
        for (Mago mago : magos) {
            int acessos = mago.getAcessosRealizados();
            long totalEspera = mago.getTempoTotalEspera();
            long mediaEspera = acessos > 0 ? totalEspera / acessos : 0;
            String linha = mago.getNome()
                    + " | acessos=" + acessos
                    + " | esperaTotalMs=" + totalEspera
                    + " | esperaMediaMs=" + mediaEspera;
            relatorio.append("\n").append(linha);
            System.out.println(linha);
        }

        return relatorio.toString();
    }

    private void registrarListener(Mago mago) {
        if (magoListener == null) {
            return;
        }
        mago.setEstadoListener(estado -> magoListener.accept(mago));
    }
}
