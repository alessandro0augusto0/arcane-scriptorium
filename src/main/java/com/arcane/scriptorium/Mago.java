package com.arcane.scriptorium;

import java.util.function.Consumer;

public abstract class Mago extends Thread {
    private final int id;
    private final String nome;
    private volatile EstadoMago estadoAtual;
    protected final Grimorio grimorio;
    private final boolean cicloUnico;
    private int acessosRealizados;
    private long tempoTotalEspera;
    private volatile Consumer<EstadoMago> estadoListener;
    private volatile Consumer<String> logListener;
    private volatile Runnable fimListener;

    protected Mago(int id, String nome, Grimorio grimorio) {
        this(id, nome, grimorio, false);
    }

    protected Mago(int id, String nome, Grimorio grimorio, boolean cicloUnico) {
        this.id = id;
        this.nome = nome;
        this.grimorio = grimorio;
        this.cicloUnico = cicloUnico;
        this.estadoAtual = EstadoMago.DORMINDO;
        setName(nome + "-" + id);
    }

    public int getIdMago() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public EstadoMago getEstadoAtual() {
        return estadoAtual;
    }

    public int getAcessosRealizados() {
        return acessosRealizados;
    }

    public long getTempoTotalEspera() {
        return tempoTotalEspera;
    }

    public void setEstadoListener(Consumer<EstadoMago> estadoListener) {
        this.estadoListener = estadoListener;
    }

    public void setLogListener(Consumer<String> logListener) {
        this.logListener = logListener;
    }

    public void setFimListener(Runnable fimListener) {
        this.fimListener = fimListener;
    }

    protected void setEstadoAtual(EstadoMago novoEstado) {
        this.estadoAtual = novoEstado;
        Consumer<EstadoMago> listener = this.estadoListener;
        if (listener != null) {
            listener.accept(novoEstado);
        }
    }

    protected void registrarAcesso(long esperaMillis) {
        acessosRealizados += 1;
        tempoTotalEspera += esperaMillis;
    }

    protected void log(String mensagem) {
        Consumer<String> listener = this.logListener;
        if (listener != null) {
            listener.accept(mensagem);
        }
    }

    protected void dormir(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public final void run() {
        do {
            executarCiclo();
            if (cicloUnico) {
                break;
            }
        } while (!isInterrupted());

        Runnable listener = this.fimListener;
        if (listener != null) {
            listener.run();
        }
    }

    protected abstract void executarCiclo();
}
