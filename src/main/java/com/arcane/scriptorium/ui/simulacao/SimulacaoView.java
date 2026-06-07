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
    private StackPane regionArenaContainer;
    private HBox autoControls;
    private Button regionsOneButton;
    private Button regionsFourButton;
    private Button starvationToggle;
    private boolean cinematicModeEnabled = true;

    private enum TestScenario {
        STRESS, WRITER_PRIORITY, READER_JUSTICE, VIP_LIMIT, FAILURE_RECOVERY
    }
    private TestScenario selectedTestScenario = TestScenario.STRESS;
    private Button btnTestStress, btnTestWriter, btnTestReader, btnTestVip, btnTestFailure;
    private String testResultMessage = "";
    private javafx.scene.control.Spinner<Integer> guidedTimeSpinner;

    private SimulationEngine engine;
    private EventBus eventBus;
    private boolean isPaused = false;
    private boolean isSimulationFinished = false;
    
    private ConcurrentLinkedQueue<SimulationEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private AnimationTimer uiUpdateTimer;
    
    private ObservableList<ProcessDescriptor> waitQueueData = FXCollections.observableArrayList();
    private ObservableList<ProcessDescriptor> activeData = FXCollections.observableArrayList();
    private java.util.Map<ProcessDescriptor, Integer> activeAgentRegionMap = new java.util.concurrent.ConcurrentHashMap<>();
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
        if (currentMode == Mode.GUIDED) {
            startGuidedEnvironment();
        }
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
        if (mode == Mode.GUIDED) {
            startGuidedEnvironment();
        } else {
            handleStop();
        }
    }

    private void startGuidedEnvironment() {
        if (engine != null) {
            handleStop();
        }
        eventQueue.clear();
        if (uiUpdateTimer != null) uiUpdateTimer.stop();
        resetUI();
        
        eventBus = new EventBus();
        eventBus.addObserver(this::onSimulationEvent);
        
        lastUpdateNano = System.nanoTime();
        activeTimeSec = 0;
        
        uiUpdateTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastUpdateNano == 0) lastUpdateNano = now;
                double dt = (now - lastUpdateNano) / 1e9;
                lastUpdateNano = now;
                activeTimeSec += dt;
                processEventQueue();
            }
        };
        uiUpdateTimer.start();
        
        com.arcane.scriptorium.simulation.SimulationConfig config = 
            com.arcane.scriptorium.simulation.SimulationConfig.defaultConfig()
            .withDuration(java.time.Duration.ofHours(24));
            
        engine = SimulationEngine.automaticScenario(config, eventBus, criticalRegions, 0, 0, 0);
        engine.setStarvationPreventionEnabled(!starvationEnabled);
        engine.start();
        
        isPaused = false;
        isSimulationFinished = false;
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
        } else if (mode == Mode.AUTOMATIC || mode == Mode.CUSTOM || mode == Mode.TESTS) {
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
        
        regionArenaContainer = new StackPane();
        VBox.setVgrow(regionArenaContainer, Priority.ALWAYS);
        regionArenaContainer.getChildren().add(buildRegionArena());
        
        regionContainer.getChildren().addAll(title, subtitle, regionArenaContainer);

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
            
            VBox container = new VBox(8);
            container.setAlignment(Pos.CENTER);
            container.getChildren().add(buildRegionState("GRIMORIO CENTRAL"));
            
            javafx.scene.layout.FlowPane activeTokens = new javafx.scene.layout.FlowPane(8, 8);
            activeTokens.setAlignment(Pos.CENTER);
            for (ProcessDescriptor p : activeData) {
                if (activeTokens.getChildren().size() < 50) { // Limit to 50
                    activeTokens.getChildren().add(buildSlot(p.shortName(), p.role().displayName()));
                }
            }
            container.getChildren().add(activeTokens);
            arena.getChildren().add(container);
            return arena;
        }

        arena.setPrefSize(560, 320);
        VBox grid = new VBox(12);
        grid.setAlignment(Pos.CENTER);

        HBox rowTop = new HBox(12);
        rowTop.setAlignment(Pos.CENTER);
        rowTop.getChildren().addAll(
                buildRegionTile("AGUA", 0),
                buildRegionTile("TERRA", 1));

        HBox rowBottom = new HBox(12);
        rowBottom.setAlignment(Pos.CENTER);
        rowBottom.getChildren().addAll(
                buildRegionTile("FOGO", 2),
                buildRegionTile("AR", 3));

        grid.getChildren().addAll(rowTop, rowBottom);
        arena.getChildren().add(grid);
        return arena;
    }

    private StackPane buildRegionTile(String label, int index) {
        StackPane tile = new StackPane();
        tile.setPrefSize(240, 130);
        
        String borderColor = COLOR_ACCENT;
        if (index == 0) borderColor = "#42a5f5"; // AGUA
        else if (index == 1) borderColor = "#8d6e63"; // TERRA
        else if (index == 2) borderColor = "#ef5350"; // FOGO
        else if (index == 3) borderColor = "#bdbdbd"; // AR
        
        tile.setStyle("-fx-background-color: #0d1426;" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;");
                
        VBox container = new VBox(8);
        container.setAlignment(Pos.CENTER);
        container.getChildren().add(buildRegionState(label));
        
        javafx.scene.layout.FlowPane activeTokens = new javafx.scene.layout.FlowPane(4, 4);
        activeTokens.setAlignment(Pos.CENTER);
        
        int tokenCount = 0;
        for (int i = 0; i < activeData.size(); i++) {
            ProcessDescriptor p = activeData.get(i);
            int targetIdx = activeAgentRegionMap.getOrDefault(p, i % 4);
            if (targetIdx == index) {
                if (tokenCount < 16) {
                    activeTokens.getChildren().add(buildSlot(p.shortName(), p.role().displayName()));
                    tokenCount++;
                }
            }
        }
        
        container.getChildren().add(activeTokens);
        tile.getChildren().add(container);
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
        } else if (mode == Mode.CUSTOM) {
            return buildConfigPanelCustom(true);
        } else if (mode == Mode.AUTOMATIC) {
            return buildConfigPanelCustom(false);
        }
        return buildConfigPanelGuidedAuto();
    }

    private VBox buildConfigPanelGuidedAuto() {
        VBox panel = buildPanel("CONFIGURACOES (MODO GUIADO)");
        regionsOneButton = buildToggleChip("1 grimorio", criticalRegions == 1);
        regionsFourButton = buildToggleChip("4 grimorios", criticalRegions == 4);
        regionsOneButton.setOnAction(event -> setCriticalRegions(1));
        regionsFourButton.setOnAction(event -> setCriticalRegions(4));

        starvationToggle = buildToggleChip("Starvation: desligado", starvationEnabled);
        starvationToggle.setOnAction(event -> toggleStarvation());
        updateStarvationToggle();
        
        Button cinematicModeToggle = buildToggleChip("Modo Cinematico: ligado", cinematicModeEnabled);
        cinematicModeToggle.setOnAction(event -> toggleCinematicMode(cinematicModeToggle));
        updateCinematicToggle(cinematicModeToggle);

        Button clearQueue = buildActionButton("Limpar fila");
        clearQueue.setOnAction(e -> {
            if (engine != null) {
                for (com.arcane.scriptorium.domain.ProcessDescriptor p : waitQueueData) {
                    engine.interruptAgent(p.id());
                }
            }
        });
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
                buildLabelLine("Visuais"),
                cinematicModeToggle,
                buildSeparator(),
                buildLabelLine("Tempo de Acesso"),
                buildSpinnerRow("Duracao (seg)", guidedTimeSpinner = createSpinner(5)),
                buildSeparator(),
                buildLabelLine("Fila"),
                clearQueue,
                buildSeparator(),
                buildLabelLine("Controles"),
                resetBtn);
        return panel;
    }

    private VBox buildConfigPanelCustom(boolean includeTimer) {
        VBox panel = buildPanel(includeTimer ? "CONFIGURACOES (MODO CUSTOM)" : "CONFIGURACOES (MODO AUTOMATICO)");
        regionsOneButton = buildToggleChip("1 grimorio", criticalRegions == 1);
        regionsFourButton = buildToggleChip("4 grimorios", criticalRegions == 4);
        regionsOneButton.setOnAction(event -> setCriticalRegions(1));
        regionsFourButton.setOnAction(event -> setCriticalRegions(4));

        Button cinematicModeToggle = buildToggleChip("Modo Cinematico: ligado", cinematicModeEnabled);
        cinematicModeToggle.setOnAction(event -> toggleCinematicMode(cinematicModeToggle));
        updateCinematicToggle(cinematicModeToggle);

        Button clearQueue = buildActionButton("Limpar fila");
        clearQueue.setOnAction(e -> {
            if (engine != null) {
                for (com.arcane.scriptorium.domain.ProcessDescriptor p : waitQueueData) {
                    engine.interruptAgent(p.id());
                }
            }
        });
        Button resetBtn = buildActionButton("Resetar");
        resetBtn.setOnAction(e -> handleReset());

        panel.getChildren().addAll(
                buildLabelLine("Regioes criticas"),
                regionsOneButton,
                regionsFourButton,
                buildSeparator(),
                buildLabelLine("Visuais"),
                cinematicModeToggle,
                buildSeparator(),
                buildLabelLine("Quantidade de Magos"),
                buildSpinnerRow("Leitores comuns", commonSpinner = createSpinner(15)),
                buildSpinnerRow("Leitores criticos", criticalSpinner = createSpinner(5)),
                buildSpinnerRow("Escritores", writerSpinner = createSpinner(5))
        );
        
        if (includeTimer) {
            panel.getChildren().addAll(
                buildSeparator(),
                buildLabelLine("Tempo Maximo"),
                buildSpinnerRow("Duração (seg)", timerSpinner = createSpinner(30))
            );
        }
        
        panel.getChildren().addAll(
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
        VBox panel = buildPanel("CONFIGURACOES (MODO TESTES)");
        regionsOneButton = buildToggleChip("1 grimorio", criticalRegions == 1);
        regionsFourButton = buildToggleChip("4 grimorios", criticalRegions == 4);
        regionsOneButton.setOnAction(event -> setCriticalRegions(1));
        regionsFourButton.setOnAction(event -> setCriticalRegions(4));

        Button cinematicModeToggle = buildToggleChip("Modo Cinematico: ligado", cinematicModeEnabled);
        cinematicModeToggle.setOnAction(event -> toggleCinematicMode(cinematicModeToggle));
        updateCinematicToggle(cinematicModeToggle);

        Button resetBtn = buildActionButton("Resetar");
        resetBtn.setOnAction(e -> handleReset());

        btnTestStress = buildToggleChip("Teste de estresse", selectedTestScenario == TestScenario.STRESS);
        btnTestWriter = buildToggleChip("Prioridade ao escritor", selectedTestScenario == TestScenario.WRITER_PRIORITY);
        btnTestReader = buildToggleChip("Justica para leitores", selectedTestScenario == TestScenario.READER_JUSTICE);
        btnTestVip = buildToggleChip("Limite de prioridade VIP", selectedTestScenario == TestScenario.VIP_LIMIT);
        btnTestFailure = buildToggleChip("Recuperacao de falhas", selectedTestScenario == TestScenario.FAILURE_RECOVERY);

        btnTestStress.setOnAction(e -> selectTestScenario(TestScenario.STRESS));
        btnTestWriter.setOnAction(e -> selectTestScenario(TestScenario.WRITER_PRIORITY));
        btnTestReader.setOnAction(e -> selectTestScenario(TestScenario.READER_JUSTICE));
        btnTestVip.setOnAction(e -> selectTestScenario(TestScenario.VIP_LIMIT));
        btnTestFailure.setOnAction(e -> selectTestScenario(TestScenario.FAILURE_RECOVERY));

        panel.getChildren().addAll(
                buildLabelLine("Regioes criticas"),
                regionsOneButton,
                regionsFourButton,
                buildSeparator(),
                buildLabelLine("Visuais"),
                cinematicModeToggle,
                buildSeparator(),
                buildLabelLine("Cenarios de teste"),
                btnTestStress,
                btnTestWriter,
                btnTestReader,
                btnTestVip,
                btnTestFailure,
                buildSeparator(),
                buildLabelLine("Controles"),
                resetBtn);
        return panel;
    }

    private void selectTestScenario(TestScenario scenario) {
        selectedTestScenario = scenario;
        if (btnTestStress != null) btnTestStress.setStyle(buildToggleStyle(scenario == TestScenario.STRESS));
        if (btnTestWriter != null) btnTestWriter.setStyle(buildToggleStyle(scenario == TestScenario.WRITER_PRIORITY));
        if (btnTestReader != null) btnTestReader.setStyle(buildToggleStyle(scenario == TestScenario.READER_JUSTICE));
        if (btnTestVip != null) btnTestVip.setStyle(buildToggleStyle(scenario == TestScenario.VIP_LIMIT));
        if (btnTestFailure != null) btnTestFailure.setStyle(buildToggleStyle(scenario == TestScenario.FAILURE_RECOVERY));
    }

    private void toggleStarvation() {
        starvationEnabled = !starvationEnabled;
        updateStarvationToggle();
        if (engine != null) {
            engine.setStarvationPreventionEnabled(!starvationEnabled);
        }
    }

    private void updateStarvationToggle() {
        if (starvationToggle == null) {
            return;
        }
        String label = starvationEnabled ? "Starvation: ligado" : "Starvation: desligado";
        starvationToggle.setText(label);
        starvationToggle.setStyle(buildToggleStyle(starvationEnabled));
    }

    private void toggleCinematicMode(Button toggleButton) {
        cinematicModeEnabled = !cinematicModeEnabled;
        updateCinematicToggle(toggleButton);
    }

    private void updateCinematicToggle(Button toggleButton) {
        if (toggleButton == null) return;
        String label = cinematicModeEnabled ? "Modo Cinematico: ligado" : "Modo Cinematico: desligado";
        toggleButton.setText(label);
        toggleButton.setStyle(buildToggleStyle(cinematicModeEnabled));
    }

    private void setCriticalRegions(int regions) {
        if (criticalRegions == regions) {
            return;
        }
        criticalRegions = regions;
        updateRegionToggleStyles();
        handleReset();
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
        
        Button btnAddReader = buildActionButton("Adicionar Leitor");
        Button btnAddWriter = buildActionButton("Adicionar Escritor");
        Button btnAddVip = buildActionButton("Adicionar Leitor Critico");
        
        btnAddReader.setOnAction(e -> {
            if (engine != null && guidedTimeSpinner != null) {
                engine.spawnManualCommonReader(java.time.Duration.ofSeconds(guidedTimeSpinner.getValue()));
            }
        });
        
        btnAddWriter.setOnAction(e -> {
            if (engine != null && guidedTimeSpinner != null) {
                engine.spawnManualWriter(java.time.Duration.ofSeconds(guidedTimeSpinner.getValue()));
            }
        });
        
        btnAddVip.setOnAction(e -> {
            if (engine != null && guidedTimeSpinner != null) {
                engine.spawnManualCriticalReader(java.time.Duration.ofSeconds(guidedTimeSpinner.getValue()));
            }
        });

        controls.getChildren().addAll(btnAddReader, btnAddWriter, btnAddVip);

        bottom.getChildren().addAll(controls, buildLowerPanels(false));
        return bottom;
    }

    private VBox buildBottomPanelAutomatic() {
        VBox bottom = new VBox(12);
        bottom.getChildren().add(buildLowerPanels(true));
        return bottom;
    }

    private HBox buildLowerPanels(boolean includeReport) {
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

        HBox.setHgrow(log, Priority.ALWAYS);
        lower.getChildren().addAll(metrics, log);

        if (includeReport) {
            reportPanelContent = new VBox(10);
            VBox report = buildPanel("RELATORIO AUTOMATICO");
            report.setMinWidth(260);
            
            ScrollPane reportScroll = new ScrollPane(reportPanelContent);
            reportScroll.setFitToWidth(true);
            reportScroll.setStyle("-fx-background: " + COLOR_PANEL + "; -fx-background-color: transparent; -fx-border-color: transparent;");
            reportScroll.setPrefViewportHeight(150);
            VBox.setVgrow(reportScroll, Priority.ALWAYS);
            report.getChildren().add(reportScroll);

            lower.getChildren().add(report);
        } else {
            reportPanelContent = null;
        }
        
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
        return buildMetric(label, value, COLOR_TITLE);
    }

    private HBox buildMetric(String label, String value, String valueColor) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        
        Text labelText = new Text(label + ":");
        labelText.setFont(Font.font("Serif", 12));
        labelText.setStyle("-fx-fill: " + COLOR_TEXT + ";");

        Text valueText = new Text(value);
        valueText.setFont(Font.font("Serif", FontWeight.BOLD, 12));
        valueText.setStyle("-fx-fill: " + valueColor + ";");

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
        activeData.clear();
        activeAgentRegionMap.clear();
        if (regionArenaContainer != null) {
            regionArenaContainer.getChildren().clear();
            regionArenaContainer.getChildren().add(buildRegionArena());
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
                buildMetric("Escritor Ativo", "Nao"),
                buildMetric("Starvation Detectada", "Nao")
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
        if (engine != null && isPaused && !isSimulationFinished) {
            isPaused = false;
            engine.resume();
            return;
        }
        if (engine != null && !isSimulationFinished) {
            return;
        }
        
        isSimulationFinished = false;

        if (currentMode == Mode.TESTS) {
            runTestScenario();
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
        engine.setStarvationPreventionEnabled(!starvationEnabled);
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

    private void runTestScenario() {
        eventQueue.clear();
        if (uiUpdateTimer != null) uiUpdateTimer.stop();
        resetUI();
        
        eventBus = new EventBus();
        eventBus.addObserver(this::onSimulationEvent);
        
        lastUpdateNano = System.nanoTime();
        activeTimeSec = 0;
        
        uiUpdateTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastUpdateNano == 0) lastUpdateNano = now;
                double dt = (now - lastUpdateNano) / 1e9;
                lastUpdateNano = now;
                activeTimeSec += dt;
                processEventQueue();
            }
        };
        uiUpdateTimer.start();
        
        isPaused = false;
        isSimulationFinished = false;

        Thread testThread = new Thread(() -> {
            testResultMessage = "⏳ Executando: " + selectedTestScenario.name() + "...\n";
            boolean passed = true;
            try {
                switch (selectedTestScenario) {
                    case STRESS -> com.arcane.scriptorium.validation.StandaloneConcurrencyValidation.stressTestMaintainsMutualExclusionAndInternalCounters(eventBus);
                    case WRITER_PRIORITY -> com.arcane.scriptorium.validation.StandaloneConcurrencyValidation.writerWaitingClosesTurnstileForLateCommonReaders(eventBus);
                    case READER_JUSTICE -> com.arcane.scriptorium.validation.StandaloneConcurrencyValidation.commonReadersWaitingBehindWriterReceiveBoundedTurnAfterWriter(eventBus);
                    case VIP_LIMIT -> com.arcane.scriptorium.validation.StandaloneConcurrencyValidation.criticalReaderVipLimitForcesWriterBeforeNextCriticalReader(eventBus);
                    case FAILURE_RECOVERY -> com.arcane.scriptorium.validation.StandaloneConcurrencyValidation.waitingCountersRecoverAfterInterruptions(eventBus);
                }
            } catch (Throwable error) {
                passed = false;
                testResultMessage += "\n❌ FALHA NO TESTE!\nMotivo: " + error.getMessage();
                error.printStackTrace();
            } finally {
                if (passed) {
                    testResultMessage += "\n✅ TESTE CONCLUIDO COM SUCESSO!\nTodas as invariantes validadas.";
                }
                Platform.runLater(this::handleStop);
            }
        });
        testThread.setDaemon(true);
        testThread.start();
    }

    private void handleStop() {
        if (isSimulationFinished) return;
        if (isPaused) {
            handlePause(null);
        }
        if (engine != null) {
            engine.stop();
        }
        isSimulationFinished = true;
        if (uiUpdateTimer != null) {
            uiUpdateTimer.stop();
        }
        
        // Process remaining queue
        processEventQueue();
        
        String reportStr = "Relatorio finalizado.";
        if (currentMode == Mode.TESTS) {
            reportStr = testResultMessage;
        } else if (engine != null) {
            reportStr = engine.metricsReport();
            
            com.arcane.scriptorium.synchronization.SynchronizationSnapshot latestSnap = engine.finalSnapshot();
            if (latestSnap != null) {
                boolean isStarving = starvationEnabled && latestSnap.waitingWriters() > 0 && latestSnap.activeReaders() > 0;
                if (isStarving) {
                    reportStr += "\n\n[ALERTA CRITICO]\nStarvation Detectada: SIM (ESCRITOR PRESO!)";
                }
            }
        }
        
        final String finalReportStr = reportStr;
        
        Platform.runLater(() -> {
            if (reportPanelContent != null) {
                reportPanelContent.getChildren().clear();
                Text reportText = new Text(finalReportStr);
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
        activeData.clear();
        activeAgentRegionMap.clear();
        isPaused = false;
        isSimulationFinished = false;
        
        resetUI();
        if (currentMode == Mode.GUIDED) {
            startGuidedEnvironment();
        }
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
        boolean regionUpdated = false;
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
            
            // Process Queue and Region State
            if (event.process() != null) {
                if (event.type() == EventType.WAITING || event.state() == ProcessState.WAITING || event.state() == ProcessState.BLOCKED) {
                    if (!waitQueueData.contains(event.process())) {
                        waitQueueData.add(event.process());
                        queueUpdated = true;
                    }
                } else if (event.type() == EventType.ENTERED) {
                    if (waitQueueData.contains(event.process())) {
                        waitQueueData.remove(event.process());
                        queueUpdated = true;
                    }
                    if (!activeData.contains(event.process())) {
                        activeData.add(event.process());
                        regionUpdated = true;
                    }
                } else if (event.type() == EventType.STATE && (event.state() == ProcessState.READING || event.state() == ProcessState.WRITING)) {
                    if (event.message() != null && event.process() != null) {
                        String msg = event.message().toUpperCase();
                        int regionIndex = 0; // fallback to 0
                        if (msg.contains("AGUA")) regionIndex = 0;
                        else if (msg.contains("TERRA")) regionIndex = 1;
                        else if (msg.contains("FOGO")) regionIndex = 2;
                        else if (msg.contains("AR")) regionIndex = 3;
                        
                        // Set specific target
                        activeAgentRegionMap.put(event.process(), regionIndex);
                        regionUpdated = true;
                    }
                } else if (event.type() == EventType.EXITED || event.state() == ProcessState.RESTING || event.state() == ProcessState.STOPPED) {
                    if (waitQueueData.contains(event.process())) {
                        waitQueueData.remove(event.process());
                        queueUpdated = true;
                    }
                    if (activeData.contains(event.process())) {
                        activeData.remove(event.process());
                        activeAgentRegionMap.remove(event.process());
                        regionUpdated = true;
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
        
        if (regionUpdated && regionArenaContainer != null) {
            regionArenaContainer.getChildren().clear();
            regionArenaContainer.getChildren().add(buildRegionArena());
        }
        
        if (latestSnap != null && metricsPanelContent != null) {
            boolean isStarving = starvationEnabled && latestSnap.waitingWriters() > 0 && latestSnap.activeReaders() > 0;
            String starvationText = isStarving ? "SIM (ESCRITOR PRESO!)" : "Nao";
            String starvationColor = isStarving ? "#ff4444" : COLOR_TITLE;

            metricsPanelContent.getChildren().clear();
            metricsPanelContent.getChildren().addAll(
                buildMetric("Leituras (Comuns + Criticas)", String.valueOf(latestSnap.completedReads())),
                buildMetric("Escritas (Realizadas)", String.valueOf(latestSnap.completedWrites())),
                buildMetric("Engarrafamento (Fila Comum)", latestSnap.waitingCommonReaders() + " magos"),
                buildMetric("Engarrafamento (Fila Critica)", latestSnap.waitingCriticalReaders() + " magos"),
                buildMetric("Engarrafamento (Fila Escritor)", latestSnap.waitingWriters() + " magos"),
                buildMetric("Acessos Ativos (Leitores)", String.valueOf(latestSnap.activeReaders())),
                buildMetric("Escritor Ativo", latestSnap.writerActive() ? "Sim" : "Nao"),
                buildMetric("Starvation Detectada", starvationText, starvationColor)
            );
        }
    }
}
