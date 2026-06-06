module com.arcane.scriptorium {
    requires javafx.controls;
    requires javafx.media;
    requires transitive javafx.graphics;
    requires com.github.librepdf.openpdf;
    requires java.desktop;

    exports com.arcane.scriptorium;
    exports com.arcane.scriptorium.domain;
    exports com.arcane.scriptorium.events;
    exports com.arcane.scriptorium.simulation;
    exports com.arcane.scriptorium.synchronization;
    exports com.arcane.scriptorium.ui.console;
    exports com.arcane.scriptorium.ui;
    exports com.arcane.scriptorium.ui.menu;
    exports com.arcane.scriptorium.ui.regras;
    exports com.arcane.scriptorium.ui.simulacao;
    exports com.arcane.scriptorium.validation;

    opens com.arcane.scriptorium.ui to javafx.graphics;
    opens com.arcane.scriptorium.ui.menu to javafx.graphics;
    opens com.arcane.scriptorium.ui.regras to javafx.graphics;
    opens com.arcane.scriptorium.ui.simulacao to javafx.graphics;
}
