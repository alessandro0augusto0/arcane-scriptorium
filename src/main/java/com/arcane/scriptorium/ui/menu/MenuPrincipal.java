package com.arcane.scriptorium.ui.menu;

import com.arcane.scriptorium.ui.UiIcon;
import com.arcane.scriptorium.ui.regras.RegrasView;
import com.arcane.scriptorium.ui.simulacao.SimulacaoView;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class MenuPrincipal {
    private static final double WIDTH = 1280;
    private static final double HEIGHT = 720;

    private static final String VIDEO_RESOURCE = "/videos/menu_background.mp4";
    private static final String IMAGE_FALLBACK_RESOURCE = "/videos/menu_background.jpg";
    private static final String VIDEO_DEV_PATH = "src/main/resources/videos/menu_background.mp4";
    private static final String IMAGE_DEV_PATH = "src/main/resources/videos/menu_background.jpg";

    private static final boolean DEBUG_BUTTONS = false;
    private static final double BTN_X = 90;
    private static final double BTN_WIDTH = 380;
    private static final double BTN_HEIGHT = 70;
    private static final double BTN_START_Y = 300;
    private static final double BTN_RULES_Y = 380;
    private static final double BTN_EXIT_Y = 460;

    private final Stage stage;
    private MediaPlayer mediaPlayer;

    public MenuPrincipal(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        StackPane loadingRoot = buildLoadingScreen();
        Scene scene = new Scene(loadingRoot, WIDTH, HEIGHT);
        UiIcon.apply(stage);
        stage.setTitle("Biblioteca Arcana - Menu Principal");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setOnCloseRequest(event -> stopVideo());
        stage.show();

        preloadMediaAndShow();
    }

    private StackPane buildLoadingScreen() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #0d0d1a;");

        Text title = new Text("Carregando Biblioteca Arcana...");
        title.setFont(Font.font("Serif", 20));
        title.setStyle("-fx-fill: #f0c040;");

        root.getChildren().add(title);
        return root;
    }

    private void preloadMediaAndShow() {
        String uri = resolveResourceUri(VIDEO_RESOURCE, VIDEO_DEV_PATH);
        if (uri == null) {
            Platform.runLater(() -> showMainScene(false));
            return;
        }

        try {
            Media media = new Media(uri);
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setMute(false);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setAutoPlay(false);

            mediaPlayer.setOnError(() -> {
                if (mediaPlayer.getError() != null) {
                    System.err.println("[MenuPrincipal] Media error: " + mediaPlayer.getError().getMessage());
                }
                Platform.runLater(() -> showMainScene(false));
            });

            media.setOnError(() -> {
                if (media.getError() != null) {
                    System.err.println("[MenuPrincipal] Media load error: " + media.getError().getMessage());
                }
                Platform.runLater(() -> showMainScene(false));
            });

            mediaPlayer.setOnReady(() -> Platform.runLater(() -> showMainScene(true)));
        } catch (Exception ex) {
            System.err.println("[MenuPrincipal] Video load failure: " + ex.getMessage());
            Platform.runLater(() -> showMainScene(false));
        }
    }

    private void showMainScene(boolean withVideo) {
        StackPane root = new StackPane();

        if (withVideo && mediaPlayer != null) {
            MediaView view = new MediaView(mediaPlayer);
            view.setFitWidth(WIDTH);
            view.setFitHeight(HEIGHT);
            view.setPreserveRatio(false);
            root.getChildren().add(view);
        } else {
            loadFallbackImage(root);
        }

        AnchorPane buttonsLayer = buildButtonsLayer();
        root.getChildren().add(buttonsLayer);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        stage.setScene(scene);

        if (withVideo && mediaPlayer != null) {
            mediaPlayer.play();
        }
    }

    private void loadFallbackImage(StackPane root) {
        String uri = resolveResourceUri(IMAGE_FALLBACK_RESOURCE, IMAGE_DEV_PATH);
        if (uri == null) {
            root.setStyle("-fx-background-color: #1a1a2e;");
            return;
        }

        try {
            Image image = new Image(uri);
            ImageView view = new ImageView(image);
            view.setFitWidth(WIDTH);
            view.setFitHeight(HEIGHT);
            view.setPreserveRatio(false);
            root.getChildren().add(view);
        } catch (Exception ex) {
            System.err.println("[MenuPrincipal] Image load failure: " + ex.getMessage());
            root.setStyle("-fx-background-color: #1a1a2e;");
        }
    }

    private String resolveResourceUri(String classpathResource, String devPath) {
        URL resourceUrl = MenuPrincipal.class.getResource(classpathResource);
        if (resourceUrl != null) {
            return resourceUrl.toExternalForm();
        }

        Path path = Path.of(devPath);
        if (Files.exists(path)) {
            return path.toUri().toString();
        }

        return null;
    }

    private void stopVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
    }

    private AnchorPane buildButtonsLayer() {
        AnchorPane pane = new AnchorPane();
        pane.setStyle("-fx-background-color: transparent;");
        pane.setPrefSize(WIDTH, HEIGHT);

        Button start = createInvisibleButton("INICIAR SIMULACAO");
        Button rules = createInvisibleButton("REGRAS");
        Button exit = createInvisibleButton("SAIR");

        placeButton(start, BTN_X, BTN_START_Y, BTN_WIDTH, BTN_HEIGHT);
        placeButton(rules, BTN_X, BTN_RULES_Y, BTN_WIDTH, BTN_HEIGHT);
        placeButton(exit, BTN_X, BTN_EXIT_Y, BTN_WIDTH, BTN_HEIGHT);

        start.setOnAction(event -> openSimulation());
        rules.setOnAction(event -> openRules());
        exit.setOnAction(event -> confirmExit());

        pane.getChildren().addAll(start, rules, exit);
        return pane;
    }

    private Button createInvisibleButton(String text) {
        Button button = new Button(text);
        String style = DEBUG_BUTTONS
                ? "-fx-background-color: rgba(255,0,0,0.15);" +
                        "-fx-border-color: red;" +
                        "-fx-border-width: 2;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 11px;" +
                        "-fx-cursor: hand;"
                : "-fx-background-color: rgba(0,0,0,0.01);" +
                        "-fx-border-color: transparent;" +
                        "-fx-text-fill: transparent;" +
                        "-fx-cursor: hand;";

        button.setStyle(style);

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#c8ff00"));
        glow.setRadius(60);
        glow.setSpread(0.8);

        ScaleTransition enter = new ScaleTransition(Duration.millis(200), button);
        enter.setToX(1.03);
        enter.setToY(1.03);

        ScaleTransition exit = new ScaleTransition(Duration.millis(200), button);
        exit.setToX(1.0);
        exit.setToY(1.0);

        button.setOnMouseEntered(event -> {
            button.setEffect(glow);
            exit.stop();
            enter.playFromStart();
        });

        button.setOnMouseExited(event -> {
            button.setEffect(null);
            enter.stop();
            exit.playFromStart();
        });

        return button;
    }

    private void placeButton(Button button, double x, double y, double width, double height) {
        AnchorPane.setLeftAnchor(button, x);
        AnchorPane.setTopAnchor(button, y);
        button.setPrefWidth(width);
        button.setPrefHeight(height);
    }

    private void openSimulation() {
        Stage simulationStage = new Stage();
        UiIcon.apply(simulationStage);
        simulationStage.setTitle("Biblioteca Arcana - Simulacao");
        simulationStage.setResizable(false);

        SimulacaoView view = new SimulacaoView(simulationStage);
        view.show();
        simulationStage.show();
    }

    private void openRules() {
        Stage rulesStage = new Stage();
        UiIcon.apply(rulesStage);
        rulesStage.setTitle("Regras da Simulacao");
        rulesStage.setResizable(false);

        RegrasView view = new RegrasView(rulesStage);
        view.show();
        rulesStage.show();
    }

    private void confirmExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Encerrar Biblioteca Arcana");
        alert.setHeaderText(null);
        alert.setContentText("Deseja realmente encerrar a Biblioteca Arcana?");

        ButtonType confirm = new ButtonType("Sim, encerrar");
        ButtonType cancel = new ButtonType("Cancelar");
        alert.getButtonTypes().setAll(confirm, cancel);

        alert.getDialogPane().setStyle(
                "-fx-background-color: #1a1a2e;" +
                        "-fx-border-color: #9b59b6;" +
                        "-fx-border-width: 2;");
        alert.getDialogPane().lookup(".content.label").setStyle(
                "-fx-text-fill: #e8d5b7;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-family: 'Serif';");

        alert.initOwner(stage);

        Optional<ButtonType> response = alert.showAndWait();
        if (response.isPresent() && response.get() == confirm) {
            stopVideo();
            Platform.exit();
        }
    }
}
