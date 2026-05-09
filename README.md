# Arcane Scriptorium

Simulacao academica em Java para o problema dos leitores e escritores, com leitores comuns, leitores criticos e escritores disputando o acesso a um grimorio compartilhado.

## Arquitetura

```text
com.arcane.scriptorium
+-- domain            Entidades e enums do dominio
+-- events            Eventos e observadores da simulacao
+-- simulation        Ciclo de vida das threads, agentes e metricas
+-- synchronization   Coordenador de concorrencia e politica de prioridade
+-- ui.console        Renderizacao ANSI para terminal
+-- utils             Utilitarios pequenos e coesos
```

## Decisoes tecnicas

- A politica de sincronizacao fica centralizada em `ArcaneSynchronizationCoordinator`.
- As entidades concorrentes sao `Runnable`, nao subclasses diretas de `Thread`, para separar comportamento de execucao.
- O coordenador usa `ReentrantLock(true)` como mutex justo, `Condition` como filas logicas e `Semaphore` como portao da regiao critica.
- Leitores comuns compartilham a regiao critica, mas param de entrar quando ha escritor aguardando.
- Leitores criticos podem ultrapassar escritores ate o limite configurado de acessos VIP consecutivos.
- Apos um escritor executar, o privilegio VIP dos leitores criticos e restaurado.

## Compilar e executar

Com Maven e JDK 21:

```powershell
mvn compile exec:java
```

Depois que o projeto ja estiver compilado, tambem funciona:

```powershell
mvn exec:java
```

O `exec-maven-plugin` ja esta configurado para executar o modulo `com.arcane.scriptorium` e a classe `com.arcane.scriptorium.MainTerminal`.

Sem Maven, use JDK puro:

```powershell
javac -d out (Get-ChildItem -Path src/main/java -Recurse -Filter *.java).FullName
java --module-path out --module com.arcane.scriptorium/com.arcane.scriptorium.MainTerminal 15000 5
```

O primeiro argumento e a duracao da simulacao em milissegundos.
O segundo argumento e o limite maximo de leitores criticos VIP antes de um escritor ser obrigatoriamente atendido.

## Testes de concorrencia

Com Maven instalado:

```powershell
mvn test
```

Sem Maven instalado, rode a simulacao principal com JDK puro:

```powershell
javac -d out (Get-ChildItem -Path src/main/java -Recurse -Filter *.java).FullName
java --module-path out --module com.arcane.scriptorium/com.arcane.scriptorium.MainTerminal 15000 5
```

Tambem existe uma validacao standalone sem Maven e sem dependencias externas:

```powershell
javac -d out (Get-ChildItem -Path src/main/java -Recurse -Filter *.java).FullName
java --module-path out --module com.arcane.scriptorium/com.arcane.scriptorium.validation.StandaloneConcurrencyValidation
```

O documento [docs/concurrency-validation.md](docs/concurrency-validation.md) descreve os invariantes testados e as limitacoes conhecidas.
