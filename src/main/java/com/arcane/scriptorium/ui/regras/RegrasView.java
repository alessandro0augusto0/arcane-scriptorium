package com.arcane.scriptorium.ui.regras;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class RegrasView {
    private static final double WIDTH = 1200;
    private static final double HEIGHT = 800;

    private static final String COLOR_BG = "#1a1a2e";
    private static final String COLOR_TITLE = "#f0c040";
    private static final String COLOR_SUBTITLE = "#c39bd3";
    private static final String COLOR_TEXT = "#d5d8dc";
    private static final String COLOR_HIGHLIGHT = "#e8d5b7";
    private static final String COLOR_BUTTON_BG = "#4a235a";
    private static final String COLOR_BUTTON_TEXT = "#f0c040";

    private final Stage stage;

    public RegrasView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");
        root.setPadding(new Insets(20));

        root.setTop(buildHeader());
        root.setCenter(buildRulesScroll());
        root.setBottom(buildFooter());

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        stage.setScene(scene);
    }

    private VBox buildHeader() {
        Text title = new Text("REGRAS DA SIMULACAO");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 26));
        title.setStyle("-fx-fill: " + COLOR_TITLE + ";");

        Text subtitle = new Text("Problema dos Leitores e Escritores - Tanenbaum");
        subtitle.setFont(Font.font("Serif", FontWeight.NORMAL, 14));
        subtitle.setStyle("-fx-fill: " + COLOR_SUBTITLE + ";");

        VBox header = new VBox(6, title, subtitle);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 16, 0));
        header.setStyle(
                "-fx-border-color: transparent transparent #9b59b6 transparent;" +
                        "-fx-border-width: 0 0 1 0;" +
                        "-fx-padding: 0 0 16 0;");

        return header;
    }

    private ScrollPane buildRulesScroll() {
        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(16));
        contentBox.setStyle("-fx-background-color: #0d0d1a;");
        contentBox.setPrefWidth(WIDTH - 60);

        contentBox.getChildren().addAll(
                section("1. O PROBLEMA CLÁSSICO (CONTEXTO TEÓRICO)"),
                paragraph(
                        "O problema dos Leitores e Escritores é um clássico de sincronização " +
                                "concorrente formalizado por Courtois, Heymans e Parnas (1971) e " +
                                "amplamente discutido por Tanenbaum. O dilema principal surge quando " +
                                "múltiplas threads concorrentes precisam acessar um recurso compartilhado. " +
                                "Para garantir a integridade dos dados, devemos permitir múltiplas " +
                                "leituras simultâneas, mas garantir exclusão mútua absoluta durante as " +
                                "operações de escrita. Um dos maiores perigos neste cenário é a " +
                                "Inanição (Starvation), onde threads podem esperar indefinidamente " +
                                "pelo acesso ao recurso sem nunca serem atendidas."),

                section("2. A CAIXA FORTE DO GRIMÓRIO (A METÁFORA DO MONITOR)"),
                paragraph(
                        "Imagine um corredor de segurança máxima.\n" +
                        "No final dele existe uma Porta de Titânio (criticalRegionGate) protegendo o Grimório Arcano " +
                        "— a própria Região Crítica do sistema.\n\n" +
                        "Antes de alcançar a porta, todo mago precisa passar por uma sala de triagem controlada por:"),
                item("- Catraca Arcana -> policyMutex (ReentrantLock)"),
                item("- Scanner de Retina -> stateChanged + canEnter()"),
                item("- Porta de Titânio -> criticalRegionGate (Semaphore binário)"),
                item("- Câmara de Criogenia -> await() do Java Condition"),

                buildLargeImage("file:///C:/Dev/arcane-scriptorium/docs/images/arcane-library-overview.png"),

                section("3. FLUXO DE EXECUÇÃO PASSO A PASSO"),
                paragraph("1. O Primeiro Leitor"),
                paragraph(
                        "Um leitor comum entra no sistema:\n" +
                        "• bloqueia a catraca (policyMutex.lock())\n" +
                        "• passa pelo scanner (canEnter(COMMON_READER))\n" +
                        "• aumenta activeReaders\n" +
                        "• o primeiro leitor abre a porta da região crítica (criticalRegionGate.acquire())\n\n" +
                        "Enquanto houver leitores ativos, novos leitores podem compartilhar o acesso simultaneamente."),
                
                paragraph("2. O Escritor é Bloqueado"),
                paragraph(
                        "Quando um escritor chega:\n" +
                        "• ele passa pela catraca\n" +
                        "• o scanner detecta leitores ativos\n" +
                        "• o acesso é negado\n" +
                        "• waitingWriters++ ativa o alerta de prioridade\n" +
                        "• a thread entra em stateChanged.await()\n\n" +
                        "O escritor é suspenso até que a região crítica fique totalmente livre."),

                paragraph("3. Proteção Contra Starvation"),
                paragraph(
                        "Se novos leitores comuns chegarem enquanto há escritores esperando:\n" +
                        "• o scanner bloqueia novos leitores\n" +
                        "• isso impede starvation dos escritores\n" +
                        "• as threads também entram em espera (await())"),

                paragraph("4. Leitores VIP Ignoram a Fila"),
                paragraph(
                        "Leitores críticos (CRITICAL_READER) possuem prioridade especial:\n" +
                        "• conseguem entrar mesmo com escritores aguardando\n" +
                        "• respeitam apenas o limite máximo configurado\n" +
                        "• compartilham a região crítica com outros leitores\n\n" +
                        "Isso simula um sistema híbrido de prioridade."),

                paragraph("5. Saída da Região Crítica"),
                paragraph(
                        "Quando os leitores terminam:\n" +
                        "• activeReaders--\n" +
                        "• o último leitor fecha a porta (criticalRegionGate.release())\n" +
                        "• o sistema executa stateChanged.signalAll()\n\n" +
                        "Todas as threads congeladas são acordadas."),

                paragraph("6. Reavaliação das Threads"),
                paragraph(
                        "Após o signalAll():\n" +
                        "• todas as threads disputam novamente o mutex\n" +
                        "• cada uma reexecuta canEnter()\n" +
                        "• o escritor finalmente consegue exclusividade\n" +
                        "• leitores posteriores voltam para espera caso necessário"),

                section("4. OBJETIVOS DO COORDENADOR & MATRIZ DE CONCEITOS"),
                paragraph(
                        "O ArcaneSynchronizationCoordinator garante:\n" +
                        "• Exclusão mútua para escritores\n" +
                        "• Leitura concorrente segura\n" +
                        "• Prevenção de starvation\n" +
                        "• Priorização controlada para leitores críticos\n" +
                        "• Coordenação usando Semaphore, Lock e Condition"),
                item("Exclusão Mútua = Mutex (policyMutex)"),
                item("Região Crítica = criticalRegionGate"),
                item("Readers-Writers Problem = Coordenação híbrida"),
                item("Condition Variables = stateChanged.await()"),
                item("Wake-up coletivo = signalAll()"),
                item("Starvation Prevention = waitingWriters"),
                item("Threads concorrentes = Agentes arcanos"),

                section("5. ESTADOS DOS AGENTES"),
                paragraph(
                        "• Idle (Esperando): O agente está parado na fila esperando.\n" +
                        "• Frozen (Congelado): O agente está congelado na sala de espera (bloqueado aguardando liberação do acesso).\n" +
                        "• Reading / Writing (Lendo ou Escrevendo): O agente está ativamente lendo ou escrevendo no grimório."
                ),

                section("6. MANUAL DOS TOKENS ARCANOS (CATÁLOGO GRÁFICO)"),
                buildImageRow("harry_reading", "Leitor Comum Ativo", "Consultando o grimório. Múltiplos podem ler juntos."),
                buildImageRow("harry_frozen", "Leitor Comum Bloqueado", "Congelado no Monitor. Aguardando o Escritor sair ou cota de lote."),
                buildImageRow("voldemort_reading", "Leitor Crítico (VIP)", "Tem prioridade sobre novos escritores, até o limite de rajada."),
                buildImageRow("gandalf_writing", "Escritor Ativo", "Possui exclusividade absoluta. Altera a região crítica."),
                buildImageRow("gandalf_frozen", "Escritor Bloqueado", "Aguardando a sala esvaziar para entrar."),
                buildImageRow("grimoire_main", "O Grimório", "A região crítica protegida pelo Semaphore."),

                section("7. DEMONSTRAÇÕES DE TOKENS"),
                
                section("🟢 Leitores Comuns (Magos)"),
                item("Harry"),
                buildImageRow("harry_idle", "Parado na fila", "harry_idle.png"),
                buildImageRow("harry_frozen", "Congelado no Wait Set", "harry_frozen.png"),
                buildImageRow("harry_reading", "Lendo o Grimório", "harry_reading.png"),
                
                item("Ron"),
                buildImageRow("ron_idle", "Parado na fila", "ron_idle.png"),
                buildImageRow("ron_frozen", "Congelado no Wait Set", "ron_frozen.png"),
                buildImageRow("ron_reading", "Lendo o Grimório", "ron_reading.png"),

                item("Hermione"),
                buildImageRow("hermione_idle", "Parado na fila", "hermione_idle.png"),
                buildImageRow("hermione_frozen", "Congelado no Wait Set", "hermione_frozen.png"),
                buildImageRow("hermione_reading", "Lendo o Grimório", "hermione_reading.png"),

                section("🟣 Leitores Críticos - VIP (Feiticeiros)"),
                item("Voldemort"),
                buildImageRow("voldemort_idle", "Parado na fila", "voldemort_idle.png"),
                buildImageRow("voldemort_frozen", "Congelado no Wait Set", "voldemort_frozen.png"),
                buildImageRow("voldemort_reading", "Lendo o Grimório", "voldemort_reading.png"),

                item("Sauron"),
                buildImageRow("sauron_idle", "Parado na fila", "sauron_idle.png"),
                buildImageRow("sauron_frozen", "Congelado no Wait Set", "sauron_frozen.png"),
                buildImageRow("sauron_reading", "Lendo o Grimório", "sauron_reading.png"),

                section("🔴 Escritores (Anciãos)"),
                item("Gandalf"),
                buildImageRow("gandalf_idle", "Parado na fila", "gandalf_idle.png"),
                buildImageRow("gandalf_frozen", "Congelado no Wait Set", "gandalf_frozen.png"),
                buildImageRow("gandalf_writing", "Escrevendo no Grimório", "gandalf_writing.png"),

                item("Dumbledore"),
                buildImageRow("dumbledore_idle", "Parado na fila", "dumbledore_idle.png"),
                buildImageRow("dumbledore_frozen", "Congelado no Wait Set", "dumbledore_frozen.png"),
                buildImageRow("dumbledore_writing", "Escrevendo no Grimório", "dumbledore_writing.png"),

                section("📚 Grimórios (Regiões Críticas)"),
                item("Codex Umbrae (Principal)"),
                buildImageRow("grimoire_main", "Região Crítica Principal", "grimoire_main.png"),
                item("Grimório da Água"),
                buildImageRow("grimoire_water", "Domínio Elemental", "grimoire_water.png"),
                item("Grimório da Terra"),
                buildImageRow("grimoire_earth", "Domínio Elemental", "grimoire_earth.png"),
                item("Grimório do Fogo"),
                buildImageRow("grimoire_fire", "Domínio Elemental", "grimoire_fire.png"),
                item("Grimório do Ar"),
                buildImageRow("grimoire_air", "Domínio Elemental", "grimoire_air.png")
        );

        ScrollPane scroll = new ScrollPane(contentBox);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background: #0d0d1a;" +
                        "-fx-background-color: #0d0d1a;" +
                        "-fx-border-color: #9b59b6;" +
                        "-fx-border-width: 1;");
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        return scroll;
    }

    private HBox buildFooter() {
        Button close = new Button("Fechar");
        close.setStyle(
                "-fx-background-color: " + COLOR_BUTTON_BG + ";" +
                        "-fx-text-fill: " + COLOR_BUTTON_TEXT + ";" +
                        "-fx-font-family: 'Serif';" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-border-color: #9b59b6;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 8 24 8 24;" +
                        "-fx-cursor: hand;");

        close.setOnAction(event -> stage.close());

        close.setOnMouseEntered(event -> close.setStyle(
                "-fx-background-color: #6c3483;" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-font-family: 'Serif';" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-border-color: #f0c040;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 8 24 8 24;" +
                        "-fx-cursor: hand;"));
        close.setOnMouseExited(event -> close.setStyle(
                "-fx-background-color: " + COLOR_BUTTON_BG + ";" +
                        "-fx-text-fill: " + COLOR_BUTTON_TEXT + ";" +
                        "-fx-font-family: 'Serif';" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-border-color: #9b59b6;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 8 24 8 24;" +
                        "-fx-cursor: hand;"));

        HBox footer = new HBox(close);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(16, 0, 0, 0));
        return footer;
    }

    private TextFlow section(String content) {
        Text text = new Text(content);
        text.setFont(Font.font("Serif", FontWeight.BOLD, 15));
        text.setStyle("-fx-fill: " + COLOR_SUBTITLE + ";");
        return new TextFlow(text);
    }

    private TextFlow paragraph(String content) {
        Text text = new Text(content);
        text.setFont(Font.font("Serif", 13));
        text.setStyle("-fx-fill: " + COLOR_TEXT + ";");
        return new TextFlow(text);
    }

    private TextFlow item(String content) {
        Text text = new Text(content);
        text.setFont(Font.font("Serif", 13));
        text.setStyle("-fx-fill: " + COLOR_HIGHLIGHT + ";");
        TextFlow flow = new TextFlow(text);
        flow.setPadding(new Insets(0, 0, 0, 16));
        return flow;
    }

    private TextFlow code(String content) {
        Text text = new Text(content);
        text.setFont(Font.font("Monospace", 11));
        text.setStyle("-fx-fill: #7ec8e3;");
        return new TextFlow(text);
    }

    private HBox buildImageRow(String imagePath, String title, String description) {
        Image img = new Image(getClass().getResourceAsStream("/assets/" + imagePath + ".png"));
        ImageView imageView = new ImageView(img);
        imageView.setFitWidth(128);
        imageView.setFitHeight(128);

        Text titleText = new Text(title);
        titleText.setFont(Font.font("Serif", FontWeight.BOLD, 20));
        titleText.setStyle("-fx-fill: " + COLOR_HIGHLIGHT + ";");

        Text descText = new Text(description);
        descText.setFont(Font.font("Serif", 16));
        descText.setStyle("-fx-fill: " + COLOR_TEXT + ";");

        VBox textBox = new VBox(5, titleText, new TextFlow(descText));

        HBox row = new HBox(25, imageView, textBox);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildLargeImage(String url) {
        Image img = new Image(url);
        ImageView imageView = new ImageView(img);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(WIDTH - 100);

        HBox box = new HBox(imageView);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 0, 20, 0));
        return box;
    }
}
