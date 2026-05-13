package com.arcane.scriptorium.ui.simulacao;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class SimulacaoView {
    private static final double WIDTH = 1280;
    private static final double HEIGHT = 720;

    private static final String COLOR_BG = "#0d0d1a";
    private static final String COLOR_TITLE = "#f0c040";
    private static final String COLOR_SUBTITLE = "#c39bd3";
    private static final String COLOR_TEXT = "#8e9aaf";
    private static final String COLOR_BUTTON_BG = "#4a235a";
    private static final String COLOR_BUTTON_TEXT = "#f0c040";

    private final Stage stage;

    public SimulacaoView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        VBox root = new VBox(24);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        Text icon = new Text("[ simulacao ]");
        icon.setFont(Font.font("Serif", 36));
        icon.setStyle("-fx-fill: #6c3483;");

        Text title = new Text("SIMULACAO");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 42));
        title.setStyle("-fx-fill: " + COLOR_TITLE + ";");

        Text line = new Text("----------------------------");
        line.setStyle("-fx-fill: #9b59b6;");
        line.setFont(Font.font(18));

        Text subtitle = new Text("Em construcao");
        subtitle.setFont(Font.font("Serif", FontWeight.BOLD, 22));
        subtitle.setStyle("-fx-fill: " + COLOR_SUBTITLE + ";");

        Text description = new Text(
                "A simulacao de sincronizacao de acesso a grimorios\n" +
                        "esta sendo preparada pelos magos desenvolvedores.\n\n" +
                        "Em breve: visualizacao em tempo real de threads leitoras\n" +
                        "e escritoras competindo pelo acesso aos grimorios sagrados,\n" +
                        "com demonstracao de semaforos, starvation e politicas\n" +
                        "de sincronizacao.");
        description.setFont(Font.font("Serif", 16));
        description.setStyle("-fx-fill: " + COLOR_TEXT + ";");
        description.setTextAlignment(TextAlignment.CENTER);

        Button back = buildBackButton();

        Text footer = new Text("Biblioteca Arcana");
        footer.setFont(Font.font("Serif", 13));
        footer.setStyle("-fx-fill: #4a235a;");

        root.getChildren().addAll(icon, title, line, subtitle, description, back, footer);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        stage.setScene(scene);
    }

    private Button buildBackButton() {
        Button button = new Button("Voltar ao Menu");

        String normal = String.join(";",
                "-fx-background-color: " + COLOR_BUTTON_BG,
                "-fx-text-fill: " + COLOR_BUTTON_TEXT,
                "-fx-font-family: Serif",
                "-fx-font-size: 15px",
                "-fx-font-weight: bold",
                "-fx-border-color: #9b59b6",
                "-fx-border-width: 1",
                "-fx-padding: 10 32 10 32",
                "-fx-cursor: hand");

        String hover = String.join(";",
                "-fx-background-color: #6c3483",
                "-fx-text-fill: #ffffff",
                "-fx-font-family: Serif",
                "-fx-font-size: 15px",
                "-fx-font-weight: bold",
                "-fx-border-color: #f0c040",
                "-fx-border-width: 1",
                "-fx-padding: 10 32 10 32",
                "-fx-cursor: hand");

        button.setStyle(normal);
        button.setOnMouseEntered(event -> button.setStyle(hover));
        button.setOnMouseExited(event -> button.setStyle(normal));
        button.setOnAction(event -> stage.close());

        return button;
    }
}
