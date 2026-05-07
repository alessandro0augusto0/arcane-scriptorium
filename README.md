# 📚 Biblioteca Arcana: O Problema dos Leitores e Escritores

Este projeto é uma simulação interativa e temática do clássico **Problema dos Leitores e Escritores** (Readers-Writers Problem) da disciplina de Sistemas Operacionais. Ele demonstra conceitos de concorrência, sincronização de processos e exclusão mútua utilizando Java e a interface gráfica JavaFX.

## 🧙‍♂️ A Metáfora
O acesso a um recurso compartilhado (o `Grimorio`) é disputado por três tipos de processos (Threads):
* **Mago Leitor (Leitor Comum):** Pode ler o grimório simultaneamente com outros leitores. Não altera os dados.
* **Mago Escritor (Escritor):** Requer acesso **exclusivo** ao grimório para modificá-lo. Nenhum leitor ou outro escritor pode estar presente.
* **Mago Leitor Crítico (Prioridade Alta):** Uma variação do leitor comum que possui urgência na pesquisa e um escalonamento privilegiado.

## ⚙️ Arquitetura e Soluções Técnicas (Highlights)

* **Prevenção de Race Conditions (`AtomicInteger`):** O controle de leitores ativos, bem como as métricas do sistema, utilizam variáveis atômicas (`java.util.concurrent.atomic`) para garantir operações *thread-safe* sem a necessidade de blocos `synchronized` adicionais, evitando condições de corrida durante a atualização de estado.
* **Métricas em Tempo Real:** O sistema coleta dados precisos sobre a execução, gerando um relatório no encerramento (`pararSimulacao()` e interrupção limpa das threads) contendo:
  * Total de acessos realizados.
  * Tempo total de espera no estado *AGUARDANDO*.
  * Tempo médio de espera por thread (ms).
* **Solução com Catraca (*Turnstile*):** A implementação padrão do problema frequentemente sofre de inanição (*Starvation*) de escritores caso leitores continuem chegando. Para mitigar isso, utilizamos o padrão arquitetural de **Catraca**. Antes de ler, o Leitor passa por um semáforo de catraca. Se um Escritor entra na fila de espera, ele tranca a catraca, impedindo novos Leitores de entrar. Isso permite que a fila de leitores atuais esvazie e o escritor tenha sua vez de forma justa.

## ⚠️ Limitações Conhecidas (*Trade-offs* de Design)

**Risco de *Starvation* por Leitores Críticos:**
O `MagoLeitorCritico` foi desenhado para simular um processo de Sistema Operacional de prioridade máxima (Tempo Real). Para garantir seu acesso rápido, ele **ignora a Catraca**, indo direto para o semáforo de leitura principal. 

* **O efeito colateral:** Se houver um fluxo constante e ininterrupto de Leitores Críticos chegando ao sistema, os Escritores sofrerão inanição (*Starvation*), pois a exclusão mútua de escrita nunca será liberada. Esta é uma escolha de design consciente, priorizando a urgência da leitura crítica em detrimento do tempo de espera da escrita.

## 🚀 Como Executar

O projeto utiliza o **Maven Wrapper**, dispensando a instalação prévia do Apache Maven na máquina.

1. Clone o repositório.
2. Abra o terminal na raiz do projeto.
3. Certifique-se de ter o JDK 21+ instalado e configurado (`JAVA_HOME`).
4. Execute o comando abaixo para compilar e abrir a interface gráfica:

```bash
.\mvnw.cmd javafx:run