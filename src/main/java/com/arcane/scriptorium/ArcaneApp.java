package com.arcane.scriptorium;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ArcaneApp extends Application {
    private final BibliotecaController controller = new BibliotecaController();
    private final ObservableList<Mago> leitores = FXCollections.observableArrayList();
    private final ObservableList<Mago> leitoresCriticos = FXCollections.observableArrayList();
    private final ObservableList<Mago> escritores = FXCollections.observableArrayList();

    private Label statusGrimorioLabel;
    private Label leitoresAtivosLabel;
    private ListView<Mago> listaLeitores;
    private ListView<Mago> listaCriticos;
    private ListView<Mago> listaEscritores;

    @Override
    public void start(Stage stage) {
        configurarMagos();
        configurarListenerDeEstado();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0b0f1a; -fx-padding: 16;");

        root.setTop(criarPainelStatus());
        root.setCenter(criarPainelCentral());
        root.setBottom(criarPainelAcoes());

        Scene scene = new Scene(root, 1100, 700);
        stage.setTitle("Arcane Scriptorium");
        stage.setScene(scene);
        stage.show();

        atualizarStatus();
    }

    private void configurarMagos() {
        controller.adicionarLeitor(1, "Mago Leitor");
        controller.adicionarLeitor(2, "Maga Leitura");
        controller.adicionarLeitor(3, "Leitor Arcano");
        controller.adicionarLeitorCritico(4, "Mago Critico");
        controller.adicionarEscritor(5, "Ritualista");
        controller.adicionarEscritor(6, "Escriba Supremo");

        for (Mago mago : controller.getMagos()) {
            if (mago instanceof MagoLeitorCritico) {
                leitoresCriticos.add(mago);
            } else if (mago instanceof MagoEscritor) {
                escritores.add(mago);
            } else {
                leitores.add(mago);
            }
        }
    }

    private void configurarListenerDeEstado() {
        controller.setMagoListener(mago -> Platform.runLater(() -> {
            listaLeitores.refresh();
            listaCriticos.refresh();
            listaEscritores.refresh();
            atualizarStatus();
        }));
    }

    private VBox criarPainelStatus() {
        Label titulo = new Label("Biblioteca Arcana");
        titulo.setStyle("-fx-text-fill: #e6e1d3; -fx-font-size: 24px; -fx-font-weight: bold;");

        statusGrimorioLabel = new Label("Grimorio: Livre");
        statusGrimorioLabel.setStyle("-fx-text-fill: #c7c3b4; -fx-font-size: 16px;");

        leitoresAtivosLabel = new Label("Leitores ativos: 0");
        leitoresAtivosLabel.setStyle("-fx-text-fill: #c7c3b4; -fx-font-size: 16px;");

        VBox box = new VBox(6, titulo, statusGrimorioLabel, leitoresAtivosLabel);
        box.setPadding(new Insets(0, 0, 12, 0));
        return box;
    }

    private HBox criarPainelCentral() {
        listaLeitores = criarListaMagos(leitores);
        listaCriticos = criarListaMagos(leitoresCriticos);
        listaEscritores = criarListaMagos(escritores);

        VBox painelLeitores = criarPainelLista("Leitores", listaLeitores);
        VBox painelCriticos = criarPainelLista("Leitores Criticos", listaCriticos);
        VBox painelEscritores = criarPainelLista("Escritores", listaEscritores);

        HBox hbox = new HBox(16, painelLeitores, painelCriticos, painelEscritores);
        hbox.setAlignment(Pos.CENTER);
        return hbox;
    }

    private VBox criarPainelAcoes() {
        Button iniciar = new Button("Iniciar Simulacao");
        iniciar.setOnAction(event -> controller.iniciarSimulacao());

        Button parar = new Button("Parar Simulacao");
        parar.setOnAction(event -> {
            String relatorio = controller.pararSimulacao();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Relatorio de Metricas");
            alert.setHeaderText("Simulacao finalizada");
            alert.setContentText(relatorio);
            alert.showAndWait();
        });

        iniciar.setStyle("-fx-background-color: #1f6f4a; -fx-text-fill: #f1f1f1;");
        parar.setStyle("-fx-background-color: #7d2b2b; -fx-text-fill: #f1f1f1;");

        HBox box = new HBox(12, iniciar, parar);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(12, 0, 0, 0));
        return new VBox(box);
    }

    private VBox criarPainelLista(String titulo, ListView<Mago> lista) {
        Label label = new Label(titulo);
        label.setStyle("-fx-text-fill: #e6e1d3; -fx-font-size: 18px; -fx-font-weight: bold;");

        lista.setStyle("-fx-control-inner-background: #141a2b; -fx-border-color: #2f3650;");
        lista.setPrefHeight(420);

        VBox box = new VBox(8, label, lista);
        box.setPrefWidth(320);
        return box;
    }

    private ListView<Mago> criarListaMagos(ObservableList<Mago> magos) {
        ListView<Mago> listView = new ListView<>(magos);
        listView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Mago mago, boolean empty) {
                super.updateItem(mago, empty);
                if (empty || mago == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(mago.getNome() + " (" + mago.getEstadoAtual() + ")");
                setTextFill(corParaEstado(mago.getEstadoAtual()));
            }
        });
        return listView;
    }

    private void atualizarStatus() {
        boolean escrevendo = escritores.stream()
                .anyMatch(mago -> mago.getEstadoAtual() == EstadoMago.ESCREVENDO);
        boolean lendo = leitores.stream()
                .anyMatch(mago -> mago.getEstadoAtual() == EstadoMago.LENDO)
                || leitoresCriticos.stream()
                        .anyMatch(mago -> mago.getEstadoAtual() == EstadoMago.LENDO);

        String status = "Livre";
        if (escrevendo) {
            status = "Sendo Modificado";
        } else if (lendo) {
            status = "Sendo Lido";
        }

        statusGrimorioLabel.setText("Grimorio: " + status);
        leitoresAtivosLabel.setText("Leitores ativos: " + controller.getGrimorio().getLeitoresAtivos());
    }

    private Color corParaEstado(EstadoMago estado) {
        return switch (estado) {
            case DORMINDO -> Color.web("#556066");
            case AGUARDANDO_ACESSO -> Color.web("#e6a04a");
            case LENDO -> Color.web("#57d37b");
            case ESCREVENDO -> Color.web("#c44a7a");
        };
    }

    public static void main(String[] args) {
        launch(args);
    }
}
