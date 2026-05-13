package com.arcane.scriptorium.ui;

import com.arcane.scriptorium.ui.menu.MenuPrincipal;
import javafx.application.Application;
import javafx.stage.Stage;

public class GuiMain extends Application {
    @Override
    public void start(Stage primaryStage) {
        MenuPrincipal menu = new MenuPrincipal(primaryStage);
        menu.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
