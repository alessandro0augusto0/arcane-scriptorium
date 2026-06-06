package com.arcane.scriptorium.ui.simulacao;

import com.arcane.scriptorium.ui.menu.MenuPrincipal;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.arcane.scriptorium.simulation.SimulationEngine;
import com.arcane.scriptorium.simulation.SimulationConfig;
import com.arcane.scriptorium.events.EventBus;
import com.arcane.scriptorium.events.SimulationEvent;
import com.arcane.scriptorium.events.SimulationObserver;
import com.arcane.scriptorium.events.EventType;
import com.arcane.scriptorium.domain.ProcessDescriptor;
import com.arcane.scriptorium.domain.ProcessState;
import com.arcane.scriptorium.synchronization.SynchronizationSnapshot;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SimulacaoView {
    private enum Mode {
        GUIDED,
        AUTOMATIC,
        TESTS,
        CUSTOM
    }

    private static final String COLOR_BG = "#0b0f1c";
    private static final String COLOR_PANEL = "#121728";
    private static final String COLOR_PANEL_BORDER = "#3b2a4f";
    private static final String COLOR_TITLE = "#f0c040";
    private static final String COLOR_SUBTITLE = "#b8a7d9";
    private static final String COLOR_TEXT = "#9aa6c3";
    private static final String COLOR_BUTTON_BG = "#2d1d3a";
    private static final String COLOR_BUTTON_TEXT = "#f0c040";
    private static final String COLOR_ACCENT = "#5d3a7a";

    private final Stage stage;
    private Mode currentMode = Mode.GUIDED;
    private int criticalRegions = 1;
    private boolean starvationEnabled = false;
    private BorderPane root;
    private VBox queuePanel;
    private StackPane grimoirePanel;
    private HBox autoControls;
    private Button regionsOneButton;
    private Button regionsFourButton;
    private Button starvationToggle;

    private SimulationEngine engine;
    private EventBus eventBus;
    private boolean isPaused = false;
    
    private ConcurrentLinkedQueue<SimulationEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private AnimationTimer uiUpdateTimer;
    
    private ObservableList<ProcessDescriptor> waitQueueData = FXCollections.observableArrayList();
    private LinkedList<String> eventLogs = new LinkedList<>();
    
    private VBox logPanelContent;
    private VBox metricsPanelContent;
    private VBox reportPanelContent;
    
    private Spinner<Integer> commonSpinner;
    private Spinner<Integer> criticalSpinner;
    private Spinner<Integer> writerSpinner;
    private Spinner<Integer> timerSpinner;
    
    private long lastUpdateNano = 0;
    private double activeTimeSec = 0;

    public SimulacaoView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

        root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, " + COLOR_BG + ", #0a1224);");

        queuePanel = buildQueuePanel();
        grimoirePanel = buildGrimoirePanel();
        VBox configPanel = buildConfigPanel(currentMode);

        root.setTop(buildHeader());
        root.setLeft(queuePanel);
        root.setCenter(grimoirePanel);
        root.setRight(configPanel);
        applyMode(currentMode);
        BorderPane.setMargin(root.getTop(), new Insets(16, 24, 8, 24));
        BorderPane.setMargin(root.getLeft(), new Insets(8, 12, 8, 24));
        BorderPane.setMargin(root.getCenter(), new Insets(8, 12, 8, 12));
        BorderPane.setMargin(root.getRight(), new Insets(8, 24, 8, 12));
        BorderPane.setMargin(root.getBottom(), new Insets(8, 24, 16, 24));

        Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight());
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.setScene(scene);
        stage.setMaximized(true);
    }

    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        autoControls = new HBox(8);
        autoControls.setAlignment(Pos.CENTER_LEFT);
        Button play = buildTopButton("PLAY");
        Button pause = buildTopButton("PAUSE");
        Button stop = buildTopButton("STOP");
        autoControls.getChildren().addAll(play, pause, stop);

        play.setOnAction(e -> handlePlay());
        pause.setOnAction(e -> handlePause(pause));
        stop.setOnAction(e -> handleStop());

        Text title = new Text("Biblioteca Arcana");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 28));
        title.setStyle("-fx-fill: " + COLOR_TITLE + ";");

        HBox modeBox = new HBox(12);
        modeBox.setAlignment(Pos.CENTER);
        Button guided = buildTopButton("MODO GUIADO");
        Button auto = buildTopButton("MODO AUTOMATICO");
        Button tests = buildTopButton("MODO TESTES");
        Button custom = buildTopButton("MODO CUSTOM");
        modeBox.getChildren().addAll(guided, auto, tests, custom);

        guided.setOnAction(event -> setMode(Mode.GUIDED));
        auto.setOnAction(event -> setMode(Mode.AUTOMATIC));
        tests.setOnAction(event -> setMode(Mode.TESTS));
        custom.setOnAction(event -> setMode(Mode.CUSTOM));

        Button menu = buildTopButton("MENU PRINCIPAL");
        menu.setOnAction(event -> returnToMenu());

        Region spacerLeft = new Region();
        Region spacerRight = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        header.getChildren().addAll(title, spacerLeft, modeBox, spacerRight, autoControls, menu);
        return header;
    }

    private void setMode(Mode mode) {
        if (currentMode == mode) {
            return;
        }
        currentMode = mode;
        applyMode(mode);
    }

    private void applyMode(Mode mode) {
        if (autoControls != null) {
            boolean showAutoControls = mode == Mode.AUTOMATIC || mode == Mode.TESTS || mode == Mode.CUSTOM;
            autoControls.setOpacity(showAutoControls ? 1.0 : 0.0);
            autoControls.setMouseTransparent(!showAutoControls);
        }

        root.setLeft(queuePanel);
        root.setCenter(grimoirePanel);
        root.setRight(buildConfigPanel(mode));

        if (mode == Mode.GUIDED) {
            root.setBottom(buildBottomPanelGuided());
        } else if (mode == Mode.AUTOMATIC || mode == Mode.CUSTOM) {
            root.setBottom(buildBottomPanelAutomatic());
        } else {
            root.setBottom(null);
        }
    }

    private void returnToMenu() {
        Stage menuStage = new Stage();
        MenuPrincipal menu = new MenuPrincipal(menuStage);
        menu.show();
        stage.close();
    }

    private VBox queueSlotsContainer;

    private VBox buildQueuePanel() {
        VBox panel = buildPanel("FILA DE ESPERA");
        queueSlotsContainer = new VBox(8);
        
        ScrollPane scrollPane = new ScrollPane(queueSlotsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: " + COLOR_PANEL + "; -fx-background-color: transparent; -fx-border-color: transparent;");
        scrollPane.setPrefViewportHeight(200);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        panel.getChildren().addAll(
                buildHint("Magos aguardando para acessar o grimorio."),
                scrollPane);
        return panel;
    }

    private StackPane buildGrimoirePanel() {
        StackPane panel = new StackPane();
        panel.setStyle(panelStyle());
        panel.setMinWidth(520);

        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER);

        Text title = new Text("GRIMORIO CENTRAL");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 22));
        title.setStyle("-fx-fill: " + COLOR_TITLE + ";");

        Text subtitle = new Text("Regiao critica (recurso compartilhado)");
        subtitle.setFont(Font.font("Serif", 14));
        subtitle.setStyle("-fx-fill: " + COLOR_SUBTITLE + ";");

        VBox regionContainer = new VBox(12);
        regionContainer.setAlignment(Pos.CENTER);
        regionContainer.getChildren().addAll(title, subtitle, buildRegionArena());

        content.getChildren().add(regionContainer);
        panel.getChildren().add(content);
        return panel;
    }

    private StackPane buildRegionArena() {
        StackPane arena = new StackPane();
        arena.setStyle("-fx-background-color: #0f1424;" +
                "-fx-border-color: " + COLOR_ACCENT + ";" +
                "-fx-border-radius: 18;" +
                "-fx-background-radius: 18;");

        if (criticalRegions == 1) {
            arena.setPrefSize(420, 240);
            arena.getChildren().add(buildRegionState("GRIMORIO CENTRAL"));
            return arena;
        }

        arena.setPrefSize(460, 280);
        VBox grid = new VBox(12);
        grid.setAlignment(Pos.CENTER);

        HBox rowTop = new HBox(12);
        rowTop.setAlignment(Pos.CENTER);
        rowTop.getChildren().addAll(
                buildRegionTile("GRIMORIO 1"),
                buildRegionTile("GRIMORIO 2"));

        HBox rowBottom = new HBox(12);
        rowBottom.setAlignment(Pos.CENTER);
        rowBottom.getChildren().addAll(
                buildRegionTile("GRIMORIO 3"),
                buildRegionTile("GRIMORIO 4"));

        grid.getChildren().addAll(rowTop, rowBottom);
        arena.getChildren().add(grid);
        return arena;
    }

    private StackPane buildRegionTile(String label) {
        StackPane tile = new StackPane();
        tile.setPrefSize(180, 90);
        tile.setStyle("-fx-background-color: #0d1426;" +
                "-fx-border-color: " + COLOR_ACCENT + ";" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;");
        tile.getChildren().add(buildRegionState(label));
        return tile;
    }

    private Text buildRegionState(String label) {
        Text state = new Text(label);
        state.setFont(Font.font("Serif", FontWeight.BOLD, 14));
        state.setStyle("-fx-fill: #6fb1ff;");
        return state;
    }

    private VBox buildConfigPanel(Mode mode) {
        if (mode == Mode.TESTS) {
            return buildConfigPanelTests();
        } else if (mode == Mode.CUSTOM || mode == Mode.AUTOMATIC) {
            return buildConfigPanelCustom();
        }
        return buildConfigPanelGuidedAuto();
    }

    private VBox buildConfigPanelGuidedAuto() {
        VBox panel = buildPanel("CONFIGURACOES DA SIMULACAO");
        regionsOneButton = buildToggleChip("1 grimorio", criticalRegions == 1);
        regionsFourButton = buildToggleChip("4 grimorios", criticalRegions == 4);
        regionsOneButton.setOnAction(event -> setCriticalRegions(1));
        regionsFourButton.setOnAction(event -> setCriticalRegions(4));

        starvationToggle = buildToggleChip("Starvation: desligado", starvationEnabled);
        starvationToggle.setOnAction(event -> toggleStarvation());
        updateStarvationToggle();

        Button clearQueue = buildActionButton("Limpar fila");
        Button resetBtn = buildActionButton("Resetar");
        resetBtn.setOnAction(e -> handleReset());

        panel.getChildren().addAll(
                buildLabelLine("Regioes criticas"),
                regionsOneButton,
                regionsFourButton,
                buildSeparator(),
                buildLabelLine("Starvation"),
                starvationToggle,
                buildSeparator(),
                buildLabelLine("Fila"),
                clearQueue,
                buildSeparator(),
                buildLabelLine("Controles"),
                resetBtn);
        return panel;
    }

    private VBox buildConfigPanelCustom() {
        VBox panel = buildPanel("CONFIGURACOES DA SIMULACAO");
        regionsOneButton = buildToggleChip("1 grimorio", criticalRegions == 1);
        regionsFourButton = buildToggleChip("4 grimorios", criticalRegions == 4);
        regionsOneButton.setOnAction(event -> setCriticalRegions(1));
        regionsFourButton.setOnAction(event -> setCriticalRegions(4));

        starvationToggle = buildToggleChip("Starvation: desligado", starvationEnabled);
        starvationToggle.setOnAction(event -> toggleStarvation());
        updateStarvationToggle();

        Button clearQueue = buildActionButton("Limpar fila");
        Button resetBtn = buildActionButton("Resetar");
        resetBtn.setOnAction(e -> handleReset());

        panel.getChildren().addAll(
                buildLabelLine("Regioes criticas"),
                regionsOneButton,
                regionsFourButton,
                buildSeparator(),
                buildLabelLine("Starvation"),
                starvationToggle,
                buildSeparator(),
                buildLabelLine("Quantidade de Magos"),
                buildSpinnerRow("Leitores comuns", commonSpinner = createSpinner(15)),
                buildSpinnerRow("Leitores criticos", criticalSpinner = createSpinner(5)),
                buildSpinnerRow("Escritores", writerSpinner = createSpinner(5)),
                buildSeparator(),
                buildLabelLine("Tempo Maximo"),
                buildSpinnerRow("Duração (seg)", timerSpinner = createSpinner(30)),
                buildSeparator(),
                buildLabelLine("Fila"),
                clearQueue,
                buildSeparator(),
                buildLabelLine("Controles"),
                resetBtn);
        return panel;
    }

    private Spinner<Integer> createSpinner(int defaultValue) {
        Spinner<Integer> spinner = new Spinner<>(0, 1000, defaultValue);
        spinner.setEditable(true);
        spinner.setPrefWidth(70);
        spinner.setStyle("-fx-base: " + COLOR_BUTTON_BG + ";");
        return spinner;
    }

    private HBox buildSpinnerRow(String label, Spinner<Integer> spinner) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        
        Text text = new Text(label);
        text.setFont(Font.font("Serif", 12));
        text.setStyle("-fx-fill: " + COLOR_TEXT + ";");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        row.getChildren().addAll(text, spacer, spinner);
        return row;
    }

    private VBox buildConfigPanelTests() {
        VBox panel = buildPanel("CONFIGURACOES DA SIMULACAO");
        regionsOneButton = buildToggleChip("1 grimorio", criticalRegions == 1);
        regionsFourButton = buildToggleChip("4 grimorios", criticalRegions == 4);
        regionsOneButton.setOnAction(event -> setCriticalRegions(1));
        regionsFourButton.setOnAction(event -> setCriticalRegions(4));

        Button resetBtn = buildActionButton("Resetar");
        resetBtn.setOnAction(e -> handleReset());

        panel.getChildren().addAll(
                buildLabelLine("Regioes criticas"),
                regionsOneButton,
                regionsFourButton,
                buildSeparator(),
                buildLabelLine("Cenarios de teste"),
                buildToggleChip("Teste de estresse", false),
                buildToggleChip("Prioridade ao escritor", false),
                buildToggleChip("Justica para leitores", false),
                buildToggleChip("Limite de prioridade VIP", false),
                buildToggleChip("Recuperacao de falhas", false),
                buildSeparator(),
                buildLabelLine("Controles"),
                resetBtn);
        return panel;
    }

    private void toggleStarvation() {
        starvationEnabled = !starvationEnabled;
        updateStarvationToggle();
    }

    private void updateStarvationToggle() {
        if (starvationToggle == null) {
            return;
        }
        String label = starvationEnabled ? "Starvation: ligado" : "Starvation: desligado";
        starvationToggle.setText(label);
        starvationToggle.setStyle(buildToggleStyle(starvationEnabled));
    }

    private void setCriticalRegions(int regions) {
        if (criticalRegions == regions) {
            return;
        }
        criticalRegions = regions;
        updateRegionToggleStyles();
        grimoirePanel.getChildren().clear();
        grimoirePanel.getChildren().add(buildGrimoirePanel().getChildren().get(0));
    }

    private void updateRegionToggleStyles() {
        if (regionsOneButton != null) {
            regionsOneButton.setStyle(buildToggleStyle(criticalRegions == 1));
        }
        if (regionsFourButton != null) {
            regionsFourButton.setStyle(buildToggleStyle(criticalRegions == 4));
        }
    }

    private VBox buildBottomPanelGuided() {
        VBox bottom = new VBox(12);

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getChildren().addAll(
                buildActionButton("Adicionar Leitor"),
                buildActionButton("Adicionar Escritor"),
                buildActionButton("Adicionar Leitor Critico"));

        bottom.getChildren().addAll(controls, buildLowerPanels());
        return bottom;
    }

    private VBox buildBottomPanelAutomatic() {
        VBox bottom = new VBox(12);
        bottom.getChildren().add(buildLowerPanels());
        return bottom;
    }

    private HBox buildLowerPanels() {
        HBox lower = new HBox(12);
        
        metricsPanelContent = new VBox(10);
        VBox metrics = buildPanel("METRICAS EM TEMPO REAL");
        metrics.setMinWidth(360);
        metrics.getChildren().add(metricsPanelContent);

        logPanelContent = new VBox(8);
        VBox log = buildPanel("LOG DE EVENTOS");
        log.setMinWidth(420);
        
        ScrollPane logScroll = new ScrollPane(logPanelContent);
        logScroll.setFitToWidth(true);
        logScroll.setStyle("-fx-background: " + COLOR_PANEL + "; -fx-background-color: transparent; -fx-border-color: transparent;");
        logScroll.setPrefViewportHeight(150);
        VBox.setVgrow(logScroll, Priority.ALWAYS);
        log.getChildren().add(logScroll);

        reportPanelContent = new VBox(10);
        VBox report = buildPanel("RELATORIO AUTOMATICO");
        report.setMinWidth(260);
        
        ScrollPane reportScroll = new ScrollPane(reportPanelContent);
        reportScroll.setFitToWidth(true);
        reportScroll.setStyle("-fx-background: " + COLOR_PANEL + "; -fx-background-color: transparent; -fx-border-color: transparent;");
        reportScroll.setPrefViewportHeight(150);
        VBox.setVgrow(reportScroll, Priority.ALWAYS);
        report.getChildren().add(reportScroll);

        HBox.setHgrow(log, Priority.ALWAYS);

        lower.getChildren().addAll(metrics, log, report);
        
        // Setup initial placeholders
        resetUI();
        
        return lower;
    }

    private VBox buildPanel(String titleText) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(12));
        panel.setStyle(panelStyle());

        Text title = new Text(titleText);
        title.setFont(Font.font("Serif", FontWeight.BOLD, 14));
        title.setStyle("-fx-fill: " + COLOR_TITLE + ";");
        panel.getChildren().addAll(title, buildSeparator());
        return panel;
    }

    private String panelStyle() {
        return "-fx-background-color: " + COLOR_PANEL + ";" +
                "-fx-border-color: " + COLOR_PANEL_BORDER + ";" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;";
    }

    private Separator buildSeparator() {
        Separator separator = new Separator();
        separator.setStyle("-fx-opacity: 0.4;");
        return separator;
    }

    private Text buildHint(String value) {
        Text text = new Text(value);
        text.setFont(Font.font("Serif", 12));
        text.setStyle("-fx-fill: " + COLOR_TEXT + ";");
        text.setTextAlignment(TextAlignment.LEFT);
        return text;
    }

    private Text buildLabelLine(String value) {
        Text text = new Text(value);
        text.setFont(Font.font("Serif", FontWeight.BOLD, 12));
        text.setStyle("-fx-fill: " + COLOR_SUBTITLE + ";");
        return text;
    }

    private HBox buildSlot(String name, String role) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label tag = new Label(role);
        tag.setStyle("-fx-background-color: #1b2033;" +
                "-fx-text-fill: " + COLOR_TEXT + ";" +
                "-fx-padding: 2 6 2 6;" +
                "-fx-border-color: " + COLOR_ACCENT + ";" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-font-size: 11px;");

        Text label = new Text(name);
        label.setFont(Font.font("Serif", 12));
        label.setStyle("-fx-fill: " + COLOR_TEXT + ";");
        row.getChildren().addAll(label, tag);
        return row;
    }

    private HBox buildMetric(String label, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Text labelText = new Text(label + ":");
        labelText.setFont(Font.font("Serif", 12));
        labelText.setStyle("-fx-fill: " + COLOR_TEXT + ";");

        Text valueText = new Text(value);
        valueText.setFont(Font.font("Serif", FontWeight.BOLD, 12));
        valueText.setStyle("-fx-fill: " + COLOR_TITLE + ";");

        row.getChildren().addAll(labelText, valueText);
        return row;
    }

    private Label buildChip(String value) {
        Label chip = new Label(value);
        chip.setStyle("-fx-background-color: #1b2033;" +
                "-fx-text-fill: " + COLOR_TEXT + ";" +
                "-fx-padding: 4 10 4 10;" +
                "-fx-border-color: " + COLOR_ACCENT + ";" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-font-size: 11px;");
        return chip;
    }

    private Button buildToggleChip(String value, boolean active) {
        Button chip = new Button(value);
        chip.setStyle(buildToggleStyle(active));
        chip.setMaxWidth(Double.MAX_VALUE);
        chip.setAlignment(Pos.CENTER_LEFT);
        return chip;
    }

    private String buildToggleStyle(boolean active) {
        String bg = active ? "#283a5a" : "#1b2033";
        String border = active ? COLOR_TITLE : COLOR_ACCENT;
        String text = active ? COLOR_TITLE : COLOR_TEXT;
        return "-fx-background-color: " + bg + ";" +
                "-fx-text-fill: " + text + ";" +
                "-fx-padding: 4 10 4 10;" +
                "-fx-border-color: " + border + ";" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-font-size: 11px;" +
                "-fx-cursor: hand;";
    }

    private Button buildTopButton(String label) {
        Button button = new Button(label);
        button.setStyle(String.join(";",
                "-fx-background-color: " + COLOR_BUTTON_BG,
                "-fx-text-fill: " + COLOR_BUTTON_TEXT,
                "-fx-font-family: Serif",
                "-fx-font-size: 12px",
                "-fx-font-weight: bold",
                "-fx-border-color: " + COLOR_ACCENT,
                "-fx-border-width: 1",
                "-fx-padding: 8 18 8 18",
                "-fx-cursor: hand"));
        return button;
    }

    private Button buildActionButton(String label) {
        Button button = new Button(label);
        button.setStyle(String.join(";",
                "-fx-background-color: #1b2033",
                "-fx-text-fill: " + COLOR_TEXT,
                "-fx-font-family: Serif",
                "-fx-font-size: 12px",
                "-fx-border-color: " + COLOR_ACCENT,
                "-fx-border-width: 1",
                "-fx-padding: 8 16 8 16",
                "-fx-cursor: hand"));
        return button;
    }

    private void resetUI() {
        if (queueSlotsContainer != null) {
            queueSlotsContainer.getChildren().clear();
        }
        if (metricsPanelContent != null) {
            metricsPanelContent.getChildren().clear();
            metricsPanelContent.getChildren().addAll(
                buildMetric("Leituras (Comuns + Criticas)", "0"),
                buildMetric("Escritas (Realizadas)", "0"),
                buildMetric("Engarrafamento (Fila Comum)", "0 magos"),
                buildMetric("Engarrafamento (Fila Critica)", "0 magos"),
                buildMetric("Engarrafamento (Fila Escritor)", "0 magos"),
                buildMetric("Acessos Ativos (Leitores)", "0"),
                buildMetric("Escritor Ativo", "Nao")
            );
        }
        if (logPanelContent != null) {
            logPanelContent.getChildren().clear();
        }
        if (reportPanelContent != null) {
            reportPanelContent.getChildren().clear();
            reportPanelContent.getChildren().addAll(
                buildMetric("Total de Acessos Concluidos", "0"),
                buildMetric("Pior Tempo de Espera (Max)", "0.0s"),
                buildMetric("Intervencoes Anti-Starvation", "0"),
                buildMetric("Starvation Detectada", "Nao"),
                buildSeparator(),
                buildActionButton("Exportar Relatorio")
            );
        }
        waitQueueData.clear();
        eventLogs.clear();
    }

    private void handlePlay() {
        if (engine != null && isPaused) {
            isPaused = false;
            engine.resume();
            return;
        }
        if (engine != null) {
            return;
        }
        
        eventQueue.clear();
        if (uiUpdateTimer != null) {
            uiUpdateTimer.stop();
        }
        
        resetUI();
        
        eventBus = new EventBus();
        eventBus.addObserver(this::onSimulationEvent);
        
        lastUpdateNano = System.nanoTime();
        activeTimeSec = 0;
        int customTimeLimitSec = (currentMode == Mode.CUSTOM && timerSpinner != null) ? timerSpinner.getValue() : -1;
        
        uiUpdateTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastUpdateNano == 0) lastUpdateNano = now;
                double dt = (now - lastUpdateNano) / 1e9;
                lastUpdateNano = now;
                
                if (!isPaused) {
                    activeTimeSec += dt;
                    if (customTimeLimitSec > 0 && activeTimeSec >= customTimeLimitSec) {
                        handleStop();
                        return;
                    }
                }
                
                processEventQueue();
            }
        };
        uiUpdateTimer.start();
        
        SimulationConfig config = starvationEnabled ? 
            SimulationConfig.defaultConfig() : 
            SimulationConfig.defaultConfig();
            
        int commonCount = commonSpinner != null ? commonSpinner.getValue() : 15;
        int criticalCount = criticalSpinner != null ? criticalSpinner.getValue() : 5;
        int writerCount = writerSpinner != null ? writerSpinner.getValue() : 5;
            
        engine = SimulationEngine.automaticScenario(config, eventBus, criticalRegions, commonCount, criticalCount, writerCount);
        engine.start();
    }

    private void handlePause(Button pauseBtn) {
        if (engine == null) return;
        isPaused = !isPaused;
        if (isPaused) {
            engine.pause();
            if (pauseBtn != null) {
                pauseBtn.setText("RESUME");
                pauseBtn.setStyle(buildToggleStyle(true));
            }
        } else {
            engine.resume();
            if (pauseBtn != null) {
                pauseBtn.setText("PAUSE");
                pauseBtn.setStyle(String.join(";",
                    "-fx-background-color: " + COLOR_BUTTON_BG,
                    "-fx-text-fill: " + COLOR_BUTTON_TEXT,
                    "-fx-font-family: Serif",
                    "-fx-font-size: 12px",
                    "-fx-font-weight: bold",
                    "-fx-border-color: " + COLOR_ACCENT,
                    "-fx-border-width: 1",
                    "-fx-padding: 8 18 8 18",
                    "-fx-cursor: hand"));
            }
        }
    }

    private void handleStop() {
        if (engine == null) return;
        if (isPaused) {
            handlePause(null);
        }
        engine.stop();
        if (uiUpdateTimer != null) {
            uiUpdateTimer.stop();
        }
        
        // Process remaining queue
        processEventQueue();
        
        String reportStr = engine.metricsReport();
        
        Platform.runLater(() -> {
            if (reportPanelContent != null) {
                reportPanelContent.getChildren().clear();
                Text reportText = new Text(reportStr);
                reportText.setFont(Font.font("Monospaced", 11));
                reportText.setStyle("-fx-fill: " + COLOR_TEXT + ";");
                
                Button btnExport = buildActionButton("Exportar PDF");
                btnExport.setStyle("-fx-base: #673AB7; -fx-text-fill: white; -fx-font-weight: bold;");
                btnExport.setOnAction(e -> handleExportPdf());
                
                reportPanelContent.getChildren().addAll(reportText, btnExport);
            }
        });
        
        isPaused = false;
    }

    private void handleReset() {
        if (engine != null) {
            if (isPaused) {
                handlePause(null);
            }
            engine.stop();
            engine = null;
        }
        if (uiUpdateTimer != null) {
            uiUpdateTimer.stop();
        }
        eventQueue.clear();
        eventLogs.clear();
        waitQueueData.clear();
        isPaused = false;
        
        resetUI();
    }
    
    private void handleExportPdf() {
        if (engine == null) return;
        
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Salvar Relatorio PDF");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("Relatorio_Simulacao_Arcana.pdf");
        
        java.io.File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                PdfReportGenerator.generateReport(file, engine);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void onSimulationEvent(SimulationEvent event) {
        eventQueue.offer(event);
    }

    private void processEventQueue() {
        if (eventQueue.isEmpty()) return;
        
        boolean queueUpdated = false;
        boolean logUpdated = false;
        SynchronizationSnapshot latestSnap = null;
        
        SimulationEvent event;
        while ((event = eventQueue.poll()) != null) {
            // Process Log
            String timestamp = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()).format(event.timestamp());
            String type = String.format("%-8s", event.type());
            String processName = event.process() != null ? event.process().shortName() : "SISTEMA";
            String state = event.state() != null ? event.state().name() : "-";
            String logMsg = "[%s] %s | %-16s | %-7s | %s".formatted(timestamp, type, processName, state, event.message());
            
            eventLogs.add(logMsg);
            if (eventLogs.size() > 50) {
                eventLogs.removeFirst();
            }
            logUpdated = true;
            
            // Process Queue State
            if (event.process() != null) {
                if (event.type() == EventType.WAITING || event.state() == ProcessState.WAITING || event.state() == ProcessState.BLOCKED) {
                    if (!waitQueueData.contains(event.process())) {
                        waitQueueData.add(event.process());
                        queueUpdated = true;
                    }
                } else if (event.type() == EventType.ENTERED || event.state() == ProcessState.RESTING || event.state() == ProcessState.STOPPED) {
                    if (waitQueueData.contains(event.process())) {
                        waitQueueData.remove(event.process());
                        queueUpdated = true;
                    }
                }
            }
            
            // Capture latest snapshot
            if (event.snapshot() != null) {
                latestSnap = event.snapshot();
            }
        }
        
        // Render Updates
        if (logUpdated) {
            logPanelContent.getChildren().clear();
            for (String log : eventLogs) {
                logPanelContent.getChildren().add(buildHint(log));
            }
        }
        
        if (queueUpdated) {
            queueSlotsContainer.getChildren().clear();
            for (ProcessDescriptor p : waitQueueData) {
                queueSlotsContainer.getChildren().add(buildSlot(p.shortName(), p.role().displayName()));
            }
        }
        
        if (latestSnap != null && metricsPanelContent != null) {
            metricsPanelContent.getChildren().clear();
            metricsPanelContent.getChildren().addAll(
                buildMetric("Leituras (Comuns + Criticas)", String.valueOf(latestSnap.completedReads())),
                buildMetric("Escritas (Realizadas)", String.valueOf(latestSnap.completedWrites())),
                buildMetric("Engarrafamento (Fila Comum)", latestSnap.waitingCommonReaders() + " magos"),
                buildMetric("Engarrafamento (Fila Critica)", latestSnap.waitingCriticalReaders() + " magos"),
                buildMetric("Engarrafamento (Fila Escritor)", latestSnap.waitingWriters() + " magos"),
                buildMetric("Acessos Ativos (Leitores)", String.valueOf(latestSnap.activeReaders())),
                buildMetric("Escritor Ativo", latestSnap.writerActive() ? "Sim" : "Nao")
            );
        }
    }
}
