package com.arcane.scriptorium.ui.regras;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class RegrasView {
    private static final double WIDTH = 800;
    private static final double HEIGHT = 600;

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
            "-fx-padding: 0 0 16 0;"
        );

        return header;
    }

    private ScrollPane buildRulesScroll() {
        TextFlow flow = new TextFlow();
        flow.setPadding(new Insets(16));
        flow.setStyle("-fx-background-color: #0d0d1a;");
        flow.setPrefWidth(WIDTH - 60);

        flow.getChildren().addAll(
            section("1. O PROBLEMA CLASSICO DOS LEITORES E ESCRITORES"),
            paragraph(
                "O problema dos Leitores e Escritores e um classico de sincronizacao " +
                "concorrente formalizado por Courtois, Heymans e Parnas em 1971 e " +
                "popularizado por Andrew S. Tanenbaum em 'Sistemas Operacionais Modernos'. " +
                "Ele modela uma situacao em que multiplas threads compartilham um recurso, " +
                "como um banco de dados, arquivo ou, neste simulador, um grimorio magico."
            ),

            section("2. PAPEIS: LEITORES E ESCRITORES"),
            paragraph(
                "LEITORES (Readers): sao threads que apenas consultam o recurso compartilhado. " +
                "Multiplos leitores podem acessar o recurso simultaneamente, pois a leitura " +
                "nao altera o estado dos dados. Na Biblioteca Arcana, os leitores sao " +
                "aprendizes de magia que consultam os grimorios ao mesmo tempo."
            ),
            paragraph(
                "ESCRITORES (Writers): sao threads que modificam o recurso compartilhado. " +
                "Um escritor exige acesso exclusivo: nenhum outro leitor ou escritor pode " +
                "estar presente enquanto ele escreve. Na Biblioteca Arcana, os escritores sao " +
                "magos que inscrevem novos feiticos nos grimorios."
            ),

            section("3. REGRAS DE SINCRONIZACAO"),
            paragraph("As seguintes invariantes devem ser respeitadas a todo momento:"),
            item("- Multiplos leitores podem ler ao mesmo tempo (leitura concorrente)."),
            item("- Apenas um escritor pode escrever por vez (exclusao mutua)."),
            item("- Leitores e escritores nao podem acessar o recurso simultaneamente."),
            item("- Nenhuma thread deve esperar indefinidamente (ausencia de starvation idealmente)."),

            section("4. O PROBLEMA DO STARVATION (INANICAO)"),
            paragraph(
                "Starvation ocorre quando uma ou mais threads nunca conseguem acesso ao " +
                "recurso porque outras threads monopolizam o acesso. Existem dois cenarios " +
                "classicos de starvation neste problema:"
            ),
            paragraph(
                "STARVATION DOS ESCRITORES: Se novos leitores chegam continuamente antes " +
                "que o ultimo leitor saia, o contador de leitores ativos nunca chega a zero. " +
                "Isso impede que qualquer escritor obtenha o mutex, podendo esperar " +
                "indefinidamente. E a politica 'Leitores tem prioridade'."
            ),
            paragraph(
                "STARVATION DOS LEITORES: Simetricamente, se novos escritores chegam " +
                "continuamente, os leitores nunca obtem acesso. Isso e mais raro em " +
                "implementacoes classicas, mas pode ocorrer com politicas de 'Escritores " +
                "tem prioridade'."
            ),

            section("5. SEMAFOROS E MECANISMOS DE CONTROLE"),
            paragraph(
                "A solucao classica de Tanenbaum utiliza dois semaforos e uma variavel " +
                "de contagem:"
            ),
            item("- mutex: semaforo binario que protege a variavel 'leitoresAtivos'."),
            item("- db (database): semaforo binario que controla o acesso exclusivo ao recurso."),
            item("- leitoresAtivos: variavel inteira que conta leitores atualmente no recurso."),
            paragraph(
                "Quando o primeiro leitor chega (leitoresAtivos vai de 0 para 1), ele executa " +
                "wait(db), bloqueando escritores. Quando o ultimo leitor sai (leitoresAtivos " +
                "volta a 0), ele executa signal(db), liberando escritores. Cada escritor " +
                "executa wait(db) para ter acesso exclusivo e signal(db) ao terminar."
            ),

            section("6. PSEUDOCODIGO DA SOLUCAO (TANENBAUM - 1A VERSAO)"),
            code(
                "// Variaveis globais\n" +
                "semaforo mutex = 1;        // protege leitoresAtivos\n" +
                "semaforo db    = 1;        // controla acesso ao recurso\n" +
                "int leitoresAtivos = 0;    // numero de leitores no recurso\n\n" +
                "// Thread LEITOR\n" +
                "leitor() {\n" +
                "  while (true) {\n" +
                "    wait(mutex);           // entra na secao critica da contagem\n" +
                "    leitoresAtivos++;\n" +
                "    if (leitoresAtivos == 1)\n" +
                "      wait(db);           // primeiro leitor bloqueia escritores\n" +
                "    signal(mutex);         // libera mutex\n\n" +
                "    // LEITURA DO RECURSO\n" +
                "    lerRecurso();\n\n" +
                "    wait(mutex);           // entra na secao critica da contagem\n" +
                "    leitoresAtivos--;\n" +
                "    if (leitoresAtivos == 0)\n" +
                "      signal(db);         // ultimo leitor libera escritores\n" +
                "    signal(mutex);         // libera mutex\n" +
                "  }\n" +
                "}\n\n" +
                "// Thread ESCRITOR\n" +
                "escritor() {\n" +
                "  while (true) {\n" +
                "    wait(db);             // espera acesso exclusivo\n" +
                "    // ESCRITA NO RECURSO\n" +
                "    escreverRecurso();\n" +
                "    signal(db);           // libera o recurso\n" +
                "  }\n" +
                "}"
            ),

            section("7. POLITICAS DE SINCRONIZACAO"),
            paragraph("Para resolver o problema do starvation dos escritores, existem politicas alternativas:"),
            item(
                "- FIFO (Fila): todas as threads sao atendidas em ordem de chegada, " +
                "sem prioridade. Justa, mas pode reduzir a concorrencia de leitores."
            ),
            item(
                "- Prioridade para Escritores: quando um escritor esta esperando, " +
                "novos leitores sao bloqueados. Elimina o starvation dos escritores, " +
                "mas pode causar starvation dos leitores."
            ),
            item(
                "- Prioridade para Leitores: comportamento padrao da 1a versao de " +
                "Tanenbaum. Leitores sempre tem precedencia. Pode causar starvation " +
                "dos escritores."
            ),
            item(
                "- Alternancia Justa: apos cada escritor, um grupo de leitores e " +
                "liberado e vice-versa, garantindo progresso para ambos os lados."
            ),

            section("8. APLICACAO NA BIBLIOTECA ARCANA"),
            paragraph(
                "Neste simulador, cada grimorio representa um recurso compartilhado. " +
                "Threads-aprendizes (leitores) consultam os grimorios para copiar feiticos. " +
                "Threads-magos (escritores) inscrevem novos feiticos e modificam o grimorio. " +
                "O simulador demonstra visualmente os estados de cada thread " +
                "(aguardando, lendo, escrevendo, concluido) e os semaforos em tempo real, " +
                "permitindo observar as situacoes de starvation e a eficacia de cada " +
                "politica de sincronizacao."
            ),

            paragraph("\n\n")
        );

        ScrollPane scroll = new ScrollPane(flow);
        scroll.setFitToWidth(true);
        scroll.setStyle(
            "-fx-background: #0d0d1a;" +
            "-fx-background-color: #0d0d1a;" +
            "-fx-border-color: #9b59b6;" +
            "-fx-border-width: 1;"
        );
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
            "-fx-cursor: hand;"
        );

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
            "-fx-cursor: hand;"
        ));
        close.setOnMouseExited(event -> close.setStyle(
            "-fx-background-color: " + COLOR_BUTTON_BG + ";" +
            "-fx-text-fill: " + COLOR_BUTTON_TEXT + ";" +
            "-fx-font-family: 'Serif';" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-border-color: #9b59b6;" +
            "-fx-border-width: 1;" +
            "-fx-padding: 8 24 8 24;" +
            "-fx-cursor: hand;"
        ));

        HBox footer = new HBox(close);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(16, 0, 0, 0));
        return footer;
    }

    private Text section(String content) {
        Text text = new Text("\n" + content + "\n");
        text.setFont(Font.font("Serif", FontWeight.BOLD, 15));
        text.setStyle("-fx-fill: " + COLOR_SUBTITLE + ";");
        return text;
    }

    private Text paragraph(String content) {
        Text text = new Text("\n" + content + "\n");
        text.setFont(Font.font("Serif", 13));
        text.setStyle("-fx-fill: " + COLOR_TEXT + ";");
        return text;
    }

    private Text item(String content) {
        Text text = new Text("\n  " + content);
        text.setFont(Font.font("Serif", 13));
        text.setStyle("-fx-fill: " + COLOR_HIGHLIGHT + ";");
        return text;
    }

    private Text code(String content) {
        Text text = new Text("\n" + content + "\n");
        text.setFont(Font.font("Monospace", 11));
        text.setStyle("-fx-fill: #7ec8e3;");
        return text;
    }
}
