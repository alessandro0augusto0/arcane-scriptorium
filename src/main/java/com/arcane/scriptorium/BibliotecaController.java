package com.arcane.scriptorium;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BibliotecaController {
    private final Grimorio grimorio;
    private final List<Mago> magos;
    private final List<MagoSpec> magoSpecs;
    private Consumer<String> logListener;

    public BibliotecaController() {
        this.grimorio = new Grimorio();
        this.magos = new ArrayList<>();
        this.magoSpecs = new ArrayList<>();
    }

    public void adicionarLeitor(int id, String nome) {
        magoSpecs.add(new MagoSpec(TipoMago.LEITOR, id, nome));
    }

    public void adicionarEscritor(int id, String nome) {
        magoSpecs.add(new MagoSpec(TipoMago.ESCRITOR, id, nome));
    }

    public void adicionarLeitorCritico(int id, String nome) {
        magoSpecs.add(new MagoSpec(TipoMago.LEITOR_CRITICO, id, nome));
    }

    public List<Mago> getMagos() {
        return new ArrayList<>(magos);
    }

    public Grimorio getGrimorio() {
        return grimorio;
    }

    public void iniciarSimulacao() {
        configurarMagos();
        for (Mago mago : magos) {
            mago.start();
        }
    }

    public void setLogListener(Consumer<String> logListener) {
        this.logListener = logListener;
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

    private void configurarMagos() {
        magos.clear();
        for (MagoSpec spec : magoSpecs) {
            Mago mago = criarMago(spec);
            registrarLogListener(mago);
            magos.add(mago);
        }
    }

    private Mago criarMago(MagoSpec spec) {
        return switch (spec.tipo) {
            case LEITOR -> new MagoLeitor(spec.id, spec.nome, grimorio);
            case LEITOR_CRITICO -> new MagoLeitorCritico(spec.id, spec.nome, grimorio);
            case ESCRITOR -> new MagoEscritor(spec.id, spec.nome, grimorio);
        };
    }

    private void registrarLogListener(Mago mago) {
        if (logListener != null) {
            mago.setLogListener(logListener);
        }
    }

    private enum TipoMago {
        LEITOR,
        LEITOR_CRITICO,
        ESCRITOR
    }

    private static final class MagoSpec {
        private final TipoMago tipo;
        private final int id;
        private final String nome;

        private MagoSpec(TipoMago tipo, int id, String nome) {
            this.tipo = tipo;
            this.id = id;
            this.nome = nome;
        }
    }
}
