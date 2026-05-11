# Validacao Formal da Concorrencia

## Suite automatizada

Os testes ficam em:

```text
src/test/java/com/arcane/scriptorium/synchronization/ArcaneSynchronizationCoordinatorTest.java
```

Eles usam JUnit 5 e validam diretamente o `ArcaneSynchronizationCoordinator`, porque ele concentra a politica concorrente do sistema.

Para ambientes sem Maven, a classe `StandaloneConcurrencyValidation` replica a validacao principal sem dependencias externas.

## Invariantes protegidos

1. Exclusao entre leitores e escritor
   - Nunca pode existir `writerActive == true` com `activeReaders > 0`.
   - O stress test tambem usa contadores atomicos externos para detectar sobreposicao real de entrada na regiao critica.

2. Exclusao entre escritores
   - O probe externo garante que `writersInside` nunca passe de 1.
   - O coordenador modela o escritor como dono exclusivo do semaforo da regiao critica.

3. Catraca contra starvation do escritor
   - Quando um escritor esta aguardando, novos leitores comuns nao podem entrar livremente.
   - O teste `writerWaitingClosesTurnstileForLateCommonReaders` segura um leitor ativo, enfileira um escritor e tenta inserir leitores comuns atras dele.

4. Ausencia de starvation em cascata dos leitores comuns
   - Apos um escritor terminar, leitores comuns que ja estavam aguardando recebem um lote limitado de atendimento antes do proximo escritor.
   - O teste `commonReadersWaitingBehindWriterReceiveBoundedTurnAfterWriter` valida essa alternancia.

5. Limite de leitores criticos VIP
   - Leitores criticos podem ultrapassar escritores apenas ate o limite configurado.
   - Com limite `1`, a ordem validada e: leitor critico, escritor, leitor critico.

6. Fairness de politica
   - A suite valida fairness por classes: escritor bloqueia novos leitores comuns; leitores comuns aguardando recebem lote apos escrita; criticos respeitam o teto VIP.
   - O sistema nao promete FIFO global absoluto entre threads individuais.

7. Deadlock
   - Os cenarios usam `assertTimeoutPreemptively`.
   - O stress test executa centenas de threads concorrentes e falha se todas nao terminarem no prazo.

8. Consistencia dos contadores internos
   - Snapshots sao inspecionados durante toda a execucao.
   - A suite falha com contadores negativos, leitor + escritor simultaneos ou estouro do limite VIP.
   - Interrupcoes de threads aguardando tambem sao testadas.

## Cenarios implementados

- Stress repetitivo com 200 threads:
  - 120 leitores comuns;
  - 50 leitores criticos;
  - 30 escritores;
  - 3 repeticoes JUnit;
  - 3 acessos por thread.

- Catraca deterministica:
  - leitor segura a regiao;
  - escritor entra na fila;
  - leitores comuns chegam depois;
  - escritor deve entrar antes dos leitores comuns tardios.

- Lote justo de leitores comuns:
  - escritor ativo;
  - segundo escritor aguardando;
  - leitores comuns aguardando;
  - apos o primeiro escritor sair, os leitores comuns ja enfileirados entram antes do segundo escritor.

- Limite VIP:
  - escritor aguardando;
  - dois leitores criticos;
  - limite configurado como `1`;
  - o segundo leitor critico deve esperar o escritor.

- Recuperacao apos interrupcao:
  - varias threads ficam aguardando;
  - todas sao interrompidas;
  - os contadores de fila precisam voltar para zero.

- Quota de lote apos interrupcao:
  - leitores comuns recebem quota apos um escritor sair;
  - parte dos leitores e interrompida antes de consumir o lote;
  - a quota precisa encolher para nao bloquear o escritor seguinte.

## Riscos concorrentes remanescentes

- A politica e justa por categoria, nao por ordem total de chegada de cada thread.
- `Condition.signalAll()` acorda todos os aguardando; isso e simples e robusto para a escala academica, mas pode gerar custo maior em cargas extremas.
- Os testes de stress aumentam muito a confianca, mas nao provam matematicamente todos os interleavings possiveis.
- A prioridade critica e limitada por rajada VIP, nao por deadline real de sistema de tempo real.
- A camada de simulacao usa tempos aleatorios; por isso os testes formais miram o coordenador, nao o loop visual.

## Melhorias futuras

- Adicionar um scheduler com tickets FIFO por classe e, se necessario, FIFO global configuravel.
- Criar testes com JCStress para explorar interleavings de baixo nivel.
- Adicionar metricas de percentis de espera, nao apenas media e maximo.
- Separar politicas em estrategias plugaveis: prioridade de escritor, prioridade de leitor, alternancia por lote, FIFO estrito.
- Adicionar propriedades configuraveis via arquivo para reproduzir cenarios especificos.
