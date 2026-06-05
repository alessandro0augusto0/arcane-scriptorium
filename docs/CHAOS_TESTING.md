# 🧨 Guia de Engenharia do Caos (Chaos Testing)

**Documentação de Resiliência da Biblioteca Arcana**

Este documento é um "Easter Egg" técnico. Ele contém instruções de como sabotar o código-fonte (desativando defesas específicas) para provar a eficácia da nossa arquitetura Híbrida (Monitor + Semáforo) e validar o tratamento de Deadlocks e Starvation.

> **⚠️ AVISO:** Não execute esses testes no código de produção. Reverta as alterações após os testes! O arquivo alvo é sempre o `ArcaneSynchronizationCoordinator.java`.

---

## Hack 1: O Colapso da Inanição (Provando a Cota de Lote)

**Objetivo:** Provar que a ausência de uma cota de loteamento justo para os leitores causa um **Deadlock (Impasse)** imediato, travando todos os magos na porta.

**A Sabotagem:**

Vá até a regra do `COMMON_READER` no método `canEnter()` e comente a validação da cota de lote:

```java
// LINHA ORIGINAL:
case COMMON_READER -> waitingWriters == 0 || commonReaderBatchQuota > 0;

// LINHA SABOTADA:
case COMMON_READER -> waitingWriters == 0; // || commonReaderBatchQuota > 0
```

**Resultado Esperado no Terminal:**

O sistema emitirá a mensagem oculta:

```
[LR] BLOCKED Aguardando lote de leitores comuns ser liberado.
```

Logo após, o sistema sofrerá um Deadlock. O Escritor ficará esperando a cota de leitores, mas os leitores serão barrados pelo próprio Monitor. A simulação travará e a métrica de acessos cairá para perto de zero.

---

## Hack 2: Teste de Defesa em Profundidade (Forçando a Quebra do Semáforo)

**Objetivo:** Provar que o Semáforo Binário (`criticalRegionGate`) atua como o Failsafe absoluto (garantia de exclusão mútua física) caso a lógica do Monitor seja corrompida.

**A Sabotagem:**

Vamos "cegar" o Monitor, fazendo ele aprovar a entrada de todas as Threads ao mesmo tempo, ignorando qualquer regra de exclusão mútua. Insira um `return true;` na primeira linha do método `canEnter()`:

```java
private boolean canEnter(AccessRole role) {
    return true; // HACK: Ignora o monitor, todas as Threads entram!

    // ...resto do código original...
}
```

**Resultado Esperado no Terminal:**

O Monitor deixará 2 processos (ex: Leitor e Escritor) passarem ao mesmo tempo. No entanto, ao tentarem pegar a chave física da região crítica (`acquireCriticalRegionGate()`), o Semáforo bloqueará a segunda Thread e o sistema lançará o erro de integridade fatal, provando que o cofre é blindado:

```
java.lang.IllegalStateException: critical region gate invariant was violated
```

---

## Hack 3: A Mensagem Fantasma (Programação Defensiva)

**Objetivo:** Acionar o fallback do método `blockReason()`, provando que o Monitor é matematicamente coeso e todas as regras de bloqueio do `canEnter()` estão perfeitamente mapeadas.

**A Sabotagem:**

Crie uma regra de bloqueio absurda no topo do método `canEnter()`, barrando um Leitor Comum mesmo que a sala esteja vazia e sem escritores esperando:

```java
private boolean canEnter(AccessRole role) {
    // HACK: Barreira injusta
    if (role == AccessRole.COMMON_READER && activeReaders == 0 && !writerActive) {
        return false;
    }
    // ...resto do código original...
}
```

**Resultado Esperado no Terminal:**

Como o método `blockReason()` não tem uma explicação para um "bloqueio de sala vazia", ele passará reto por todos os `IFs` da lógica e atingirá o `return` final de segurança imposto pelo compilador Java, imprimindo a mensagem fantasma:

```
[LR] Mago Harry #1 BLOCKED Aguardando politica de escalonamento.
```

---

> **💡 Dica do Tech Lead:** Com isso salvo no repositório, você pode abrir o VSCode durante a apresentação, mostrar o arquivo para a banca e dizer:
>
> *"Professor, se o senhor quiser ver as defesas do código agindo ao vivo, eu deixei documentado um Guia de Chaos Testing na pasta docs. Posso rodar um deles se o senhor quiser."*
>
> É o xeque-mate para tirar nota 10. Você não está apenas entregando o código — você está entregando o **manual de qualidade da engenharia**!