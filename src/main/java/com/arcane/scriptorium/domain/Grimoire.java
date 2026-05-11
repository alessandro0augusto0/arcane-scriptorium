package com.arcane.scriptorium.domain;

public final class Grimoire {
    private final String title;
    private int revision;
    private String lastInscription;

    public Grimoire(String title) {
        this.title = title;
        this.revision = 1;
        this.lastInscription = "Indice dos encantamentos estavel.";
    }

    public String read(ProcessDescriptor process) {
        return "%s consultou '%s' rev.%d: %s"
                .formatted(process.shortName(), title, revision, lastInscription);
    }

    public String write(ProcessDescriptor process) {
        revision += 1;
        lastInscription = "Runa revisada por " + process.shortName();
        return "%s atualizou '%s' para rev.%d"
                .formatted(process.shortName(), title, revision);
    }
}
