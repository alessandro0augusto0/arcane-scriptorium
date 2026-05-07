package com.arcane.scriptorium;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
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
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ArcaneApp extends Application {
    private final BibliotecaController controller = new BibliotecaController();
    private final ObservableList<Mago> leitores = FXCollections.observableArrayList();
    private final ObservableList<Mago> leitoresCriticos = FXCollections.observableArrayList();
    private final ObservableList<Mago> escritores = FXCollections.observableArrayList();

    private Stage stage;
    private Label statusGrimorioLabel;
    private Label leitoresAtivosLabel;
    private ListView<Mago> listaLeitores;
    private ListView<Mago> listaCriticos;
    private ListView<Mago> listaEscritores;
    private TextArea logArea;
    private boolean simulacaoAtiva;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Arcane Scriptorium");
        mostrarMenu();
    }

    private void configurarMagos() {
        controller.prepararModoAutomatico();
        controller.adicionarLeitor(1, "Mago Leitor");
        controller.adicionarLeitor(2, "Maga Leitura");
        controller.adicionarLeitor(3, "Leitor Arcano");
        controller.adicionarLeitorCritico(4, "Mago Critico");
        controller.adicionarEscritor(5, "Ritualista");
        controller.adicionarEscritor(6, "Escriba Supremo");
    }

    private void mostrarMenu() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("menu-root");

        Label titulo = new Label("Arcane Scriptorium");
        titulo.getStyleClass().add("menu-title");

        Label subtitulo = new Label("Forje, teste e domine a biblioteca arcana");
        subtitulo.getStyleClass().add("menu-subtitle");

        Button modoAutomatico = new Button("Modo Automatico");
        modoAutomatico.getStyleClass().add("menu-button");
        modoAutomatico.setOnAction(event -> iniciarModoAutomatico());

        Button modoManual = new Button("Modo Manual");
        modoManual.getStyleClass().add("menu-button");
        modoManual.setOnAction(event -> iniciarModoManual());

        VBox menuBox = new VBox(18, titulo, subtitulo, modoAutomatico, modoManual);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setPadding(new Insets(40));

        root.setCenter(menuBox);

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private void iniciarModoAutomatico() {
        simulacaoAtiva = false;
        configurarMagos();
        mostrarCenaPrincipal(false);
        controller.iniciarSimulacao();
        simulacaoAtiva = true;
        carregarMagosDoController();
        atualizarStatus();
    }

    private void iniciarModoManual() {
        simulacaoAtiva = false;
        controller.prepararModoManual();
        mostrarCenaPrincipal(true);
        carregarMagosDoController();
        atualizarStatus();
    }

    private void mostrarCenaPrincipal(boolean modoManual) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        root.setTop(criarPainelStatus());
        root.setCenter(criarPainelCentral());
        root.setBottom(criarPainelAcoes(modoManual));

        configurarListeners();

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private void configurarListeners() {
        controller.setMagoListener(mago -> Platform.runLater(() -> {
            listaLeitores.refresh();
            listaCriticos.refresh();
            listaEscritores.refresh();
            atualizarStatus();
        }));
        controller.setLogListener(mensagem -> Platform.runLater(() -> appendLog(mensagem)));
    }

    private VBox criarPainelStatus() {
        Label titulo = new Label("Biblioteca Arcana");
        titulo.getStyleClass().add("header-title");

        statusGrimorioLabel = new Label("Grimorio: Livre");
        statusGrimorioLabel.getStyleClass().add("status-label");

        leitoresAtivosLabel = new Label("Leitores ativos: 0");
        leitoresAtivosLabel.getStyleClass().add("status-label");

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

    private VBox criarPainelAcoes(boolean modoManual) {
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefRowCount(6);
        logArea.getStyleClass().add("log-area");

        if (modoManual) {
            Button invocarLeitor = new Button("Invocar Leitor");
            invocarLeitor.getStyleClass().add("action-neutral");
            invocarLeitor.setOnAction(event -> {
                controller.invocarLeitorManual();
                carregarMagosDoController();
                atualizarStatus();
            });

            Button invocarLeitorCritico = new Button("Invocar Leitor Critico");
            invocarLeitorCritico.getStyleClass().add("action-neutral");
            invocarLeitorCritico.setOnAction(event -> {
                controller.invocarLeitorCriticoManual();
                carregarMagosDoController();
                atualizarStatus();
            });

            Button invocarEscritor = new Button("Invocar Escritor");
            invocarEscritor.getStyleClass().add("action-neutral");
            invocarEscritor.setOnAction(event -> {
                controller.invocarEscritorManual();
                carregarMagosDoController();
                atualizarStatus();
            });

            HBox box = new HBox(12, invocarLeitor, invocarLeitorCritico, invocarEscritor);
            box.setAlignment(Pos.CENTER_RIGHT);
            box.setPadding(new Insets(8, 0, 0, 0));

            VBox painel = new VBox(10, logArea, box);
            painel.setPadding(new Insets(12, 0, 0, 0));
            return painel;
        }

        Button iniciar = new Button("Iniciar Simulacao");
        iniciar.getStyleClass().add("action-start");
        iniciar.setOnAction(event -> {
            if (simulacaoAtiva) {
                return;
            }
            controller.iniciarSimulacao();
            simulacaoAtiva = true;
            carregarMagosDoController();
            atualizarStatus();
        });

        Button parar = new Button("Parar Simulacao");
        parar.getStyleClass().add("action-stop");
        parar.setOnAction(event -> {
            String relatorio = controller.pararSimulacao();
            simulacaoAtiva = false;
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Relatorio de Metricas");
            alert.setHeaderText("Simulacao finalizada");
            alert.setContentText(relatorio);
            alert.showAndWait();
        });

        HBox box = new HBox(12, iniciar, parar);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(8, 0, 0, 0));

        VBox painel = new VBox(10, logArea, box);
        painel.setPadding(new Insets(12, 0, 0, 0));
        return painel;
    }

    private VBox criarPainelLista(String titulo, ListView<Mago> lista) {
        Label label = new Label(titulo);
        label.getStyleClass().add("panel-title");

        lista.getStyleClass().add("mage-list");
        lista.setPrefHeight(420);

        VBox box = new VBox(8, label, lista);
        box.getStyleClass().add("panel-card");
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
                    setTextFill(Color.web("#c7c3b4"));
                    setOpacity(1);
                    return;
                }

                String texto = iconeParaMago(mago)
                        + " " + mago.getNome() + " #" + mago.getIdMago()
                        + "  " + iconeParaEstado(mago.getEstadoAtual())
                        + " " + mago.getEstadoAtual();
                setText(texto);
                setTextFill(corParaEstado(mago.getEstadoAtual()));

                setOpacity(0);
                FadeTransition fade = new FadeTransition(Duration.millis(500), this);
                fade.setFromValue(0);
                fade.setToValue(1);
                fade.play();
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

    private String iconeParaEstado(EstadoMago estado) {
        return switch (estado) {
            case DORMINDO -> "💤";
            case AGUARDANDO_ACESSO -> "⏳";
            case LENDO -> "✨";
            case ESCREVENDO -> "🔥";
        };
    }

    private String iconeParaMago(Mago mago) {
        if (mago instanceof MagoLeitorCritico) {
            return "⚡";
        }
        if (mago instanceof MagoEscritor) {
            return "📜";
        }
        return "📘";
    }

    private void carregarMagosDoController() {
        leitores.clear();
        leitoresCriticos.clear();
        escritores.clear();

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

    private void appendLog(String mensagem) {
        if (logArea == null) {
            return;
        }
        logArea.appendText(mensagem + "\n");
        logArea.positionCaret(logArea.getText().length());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
