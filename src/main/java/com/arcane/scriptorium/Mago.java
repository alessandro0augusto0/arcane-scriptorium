package com.arcane.scriptorium;

public abstract class Mago extends Thread {
    private final int id;
    private final String nome;
    private volatile EstadoMago estadoAtual;
    protected final Grimorio grimorio;

    protected Mago(int id, String nome, Grimorio grimorio) {
        this.id = id;
        this.nome = nome;
        this.grimorio = grimorio;
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

    protected void setEstadoAtual(EstadoMago novoEstado) {
        this.estadoAtual = novoEstado;
    }

    protected void dormir(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
