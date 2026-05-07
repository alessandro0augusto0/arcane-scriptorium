# Prompt: Recriar Interface JavaFX (Arcane Scriptorium)

Objetivo: recriar a interface grafica JavaFX inicial com tema arcano escuro, listas de magos e logs, integrada ao backend de concorrencia.

Requisitos tecnicos:
- Projeto Maven com dependencias `javafx-controls` e `javafx-fxml`.
- `javafx-maven-plugin` configurado com `mainClass` para `com.arcane.scriptorium.ArcaneApp`.
- `module-info.java` com `requires javafx.controls`, `requires javafx.fxml` e `requires transitive javafx.graphics`.

UI (JavaFX):
- Classe `ArcaneApp` estende `Application`.
- Layout principal `BorderPane` com tema escuro via CSS.
- Topo: status do Grimorio (Livre, Sendo Lido, Sendo Modificado) e contagem de leitores ativos.
- Centro: tres listas (Leitores, Leitores Criticos, Escritores) com cores por estado.
- Rodape: `TextArea` para logs (auto-scroll) e botoes "Iniciar Simulacao" e "Parar Simulacao".

Cores e icones:
- Estados: DORMINDO = cinza escuro, AGUARDANDO = amarelo/laranja, LENDO = verde brilhante, ESCREVENDO = vermelho/purpura.
- Icones de tipo: Leitor 📘, Critico ⚡, Escritor 📜.
- Icones de estado: DORMINDO 💤, AGUARDANDO ⏳, LENDO ✨, ESCREVENDO 🔥.

Atualizacao de UI e logs:
- `Mago` deve aceitar callbacks (ex: `Consumer<EstadoMago>` e `Consumer<String>`).
- Sempre que `setEstadoAtual()` for chamado, fazer `Platform.runLater` para atualizar listas e status.
- Logs devem ser enviados pelo backend e exibidos no `TextArea`.

Comportamento:
- Corrigir `IllegalThreadStateException` recriando magos ao iniciar simulacao.
- Ao iniciar, reconstruir a lista de magos a partir de specs e atualizar `ObservableList`.

CSS:
- Criar `src/main/resources/css/style.css` com tema sombrio elegante para:
  - `.root-pane`, `.header-title`, `.status-label`, `.panel-title`
  - `.mage-list` e `.mage-list .list-cell`
  - `.log-area`
  - botoes `.action-start` e `.action-stop`

Arquivos esperados:
- `pom.xml`
- `src/main/java/com/arcane/scriptorium/ArcaneApp.java`
- `src/main/java/module-info.java`
- `src/main/resources/css/style.css`
- Ajustes em `Mago` e `BibliotecaController` para callbacks e logs
