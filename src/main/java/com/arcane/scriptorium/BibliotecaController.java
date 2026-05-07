package com.arcane.scriptorium;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BibliotecaController {
    private static final String[] NOMES_LEITORES = {
            "Frodo Baggins",
            "Samwise",
            "Harry Pointer",
            "Hermione"
    };
    private static final String[] NOMES_LEITORES_CRITICOS = {
            "Mestre Yodado",
            "Neo"
    };
    private static final String[] NOMES_ESCRITORES = {
            "Threadalf o Cinzento",
            "Albus Double-Core",
            "Ferumbytes",
            "Darth Schedulor"
    };

    private final Grimorio grimorio;
    private final List<Mago> magos;
    private final List<MagoSpec> magoSpecs;
    private Consumer<Mago> magoListener;
    private Consumer<String> logListener;
    private int proximoIdLeitor;
    private int proximoIdLeitorCritico;
    private int proximoIdEscritor;

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
        synchronized (magos) {
            return new ArrayList<>(magos);
        }
    }

    public Grimorio getGrimorio() {
        return grimorio;
    }

    public void setMagoListener(Consumer<Mago> magoListener) {
        this.magoListener = magoListener;
    }

    public void setLogListener(Consumer<String> logListener) {
        this.logListener = logListener;
    }

    public void iniciarSimulacao() {
        configurarMagos();
        for (Mago mago : magos) {
            mago.start();
        }
    }

    public void prepararModoAutomatico() {
        synchronized (magos) {
            magos.clear();
        }
        magoSpecs.clear();
    }

    public void prepararModoManual() {
        synchronized (magos) {
            magos.clear();
        }
        magoSpecs.clear();
        reiniciarGeradores();
    }

    public void invocarLeitorManual() {
        criarMagoManual(TipoMago.LEITOR);
    }

    public void invocarLeitorCriticoManual() {
        criarMagoManual(TipoMago.LEITOR_CRITICO);
    }

    public void invocarEscritorManual() {
        criarMagoManual(TipoMago.ESCRITOR);
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
        synchronized (magos) {
            magos.clear();
        }
        for (MagoSpec spec : magoSpecs) {
            Mago mago = criarMago(spec);
            registrarListeners(mago);
            synchronized (magos) {
                magos.add(mago);
            }
        }
    }

    private Mago criarMago(MagoSpec spec) {
        return criarMago(spec.tipo, spec.id, spec.nome, false);
    }

    private Mago criarMago(TipoMago tipo, int id, String nome, boolean cicloUnico) {
        return switch (tipo) {
            case LEITOR -> new MagoLeitor(id, nome, grimorio, cicloUnico);
            case LEITOR_CRITICO -> new MagoLeitorCritico(id, nome, grimorio, cicloUnico);
            case ESCRITOR -> new MagoEscritor(id, nome, grimorio, cicloUnico);
        };
    }

    private void registrarListeners(Mago mago) {
        if (magoListener != null) {
            mago.setEstadoListener(estado -> magoListener.accept(mago));
        }
        if (logListener != null) {
            mago.setLogListener(logListener);
        }
    }

    private void criarMagoManual(TipoMago tipo) {
        int id = proximoId(tipo);
        String nome = gerarNome(tipo, id);
        Mago mago = criarMago(tipo, id, nome, true);
        registrarListeners(mago);
        mago.setFimListener(() -> removerMago(mago));

        synchronized (magos) {
            magos.add(mago);
        }
        if (magoListener != null) {
            magoListener.accept(mago);
        }
        mago.start();
    }

    private void removerMago(Mago mago) {
        synchronized (magos) {
            magos.remove(mago);
        }
        if (magoListener != null) {
            magoListener.accept(mago);
        }
    }

    private int proximoId(TipoMago tipo) {
        return switch (tipo) {
            case LEITOR -> ++proximoIdLeitor;
            case LEITOR_CRITICO -> ++proximoIdLeitorCritico;
            case ESCRITOR -> ++proximoIdEscritor;
        };
    }

    private String gerarNome(TipoMago tipo, int id) {
        String[] base = switch (tipo) {
            case LEITOR -> NOMES_LEITORES;
            case LEITOR_CRITICO -> NOMES_LEITORES_CRITICOS;
            case ESCRITOR -> NOMES_ESCRITORES;
        };
        int index = Math.floorMod(id - 1, base.length);
        return base[index];
    }

    private void reiniciarGeradores() {
        proximoIdLeitor = 0;
        proximoIdLeitorCritico = 0;
        proximoIdEscritor = 0;
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
