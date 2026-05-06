package com.arcane.scriptorium;

import java.util.ArrayList;
import java.util.List;

public class BibliotecaController {
    private final Grimorio grimorio;
    private final List<Mago> magos;

    public BibliotecaController() {
        this.grimorio = new Grimorio();
        this.magos = new ArrayList<>();
    }

    public void adicionarLeitor(int id, String nome) {
        magos.add(new MagoLeitor(id, nome, grimorio));
    }

    public void adicionarEscritor(int id, String nome) {
        magos.add(new MagoEscritor(id, nome, grimorio));
    }

    public List<Mago> getMagos() {
        return new ArrayList<>(magos);
    }

    public void iniciarSimulacao() {
        for (Mago mago : magos) {
            mago.start();
        }
    }
}
