package com.arcane.scriptorium.ui;

import com.arcane.scriptorium.ui.menu.MenuPrincipal;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Classe principal para inicialização da Interface Gráfica do Arcane Scriptorium.
 * Gerencia o ciclo de vida inicial da aplicação JavaFX.
 *
 * @author Alessandro Augusto
 */
public class GuiMain extends Application {
    @Override
    public void start(Stage primaryStage) {
        UiIcon.apply(primaryStage);
        MenuPrincipal menu = new MenuPrincipal(primaryStage);
        menu.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
