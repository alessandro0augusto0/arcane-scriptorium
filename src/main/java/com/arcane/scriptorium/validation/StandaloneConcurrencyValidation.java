package com.arcane.scriptorium.validation;

import com.arcane.scriptorium.domain.AccessRole;
import com.arcane.scriptorium.domain.ProcessDescriptor;
import com.arcane.scriptorium.events.EventBus;
import com.arcane.scriptorium.events.SimulationEvent;
import com.arcane.scriptorium.events.SimulationObserver;
import com.arcane.scriptorium.synchronization.AccessPermit;
import com.arcane.scriptorium.synchronization.ArcaneSynchronizationCoordinator;
import com.arcane.scriptorium.synchronization.SynchronizationSnapshot;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Predicate;

public final class StandaloneConcurrencyValidation {
    private static final int VIP_LIMIT = 5;

    private StandaloneConcurrencyValidation() {
    }

    public static void main(String[] args) throws Exception {
        List<Failure> failures = new ArrayList<>();

        run("stressTestMaintainsMutualExclusionAndInternalCounters", 3,
                StandaloneConcurrencyValidation::stressTestMaintainsMutualExclusionAndInternalCounters,
                failures);
        run("writerWaitingClosesTurnstileForLateCommonReaders", 1,
                StandaloneConcurrencyValidation::writerWaitingClosesTurnstileForLateCommonReaders,
                failures);
        run("commonReadersWaitingBehindWriterReceiveBoundedTurnAfterWriter", 1,
                StandaloneConcurrencyValidation::commonReadersWaitingBehindWriterReceiveBoundedTurnAfterWriter,
                failures);
        run("criticalReaderVipLimitForcesWriterBeforeNextCriticalReader", 1,
                StandaloneConcurrencyValidation::criticalReaderVipLimitForcesWriterBeforeNextCriticalReader,
                failures);
        run("waitingCountersRecoverAfterInterruptions", 1,
                StandaloneConcurrencyValidation::waitingCountersRecoverAfterInterruptions,
                failures);
        run("commonReaderBatchQuotaDoesNotSurviveInterruptedReaders", 1,
                StandaloneConcurrencyValidation::commonReaderBatchQuotaDoesNotSurviveInterruptedReaders,
                failures);

        if (!failures.isEmpty()) {
            System.err.println();
            failures.forEach(failure -> {
                System.err.println("[FAIL] " + failure.name());
                failure.cause().printStackTrace(System.err);
            });
            throw new AssertionError(failures.size() + " standalone validation test(s) failed.");
        }

        System.out.println();
        System.out.println("All standalone concurrency validation tests passed.");
    }

    private static void stressTestMaintainsMutualExclusionAndInternalCounters() throws Exception {
        RecordingObserver observer = new RecordingObserver();
        EventBus eventBus = new EventBus();
        eventBus.addObserver(observer);
        ArcaneSynchronizationCoordinator coordinator =
                new ArcaneSynchronizationCoordinator(VIP_LIMIT, eventBus);

        CriticalRegionProbe probe = new CriticalRegionProbe();
        Queue<String> violations = new ConcurrentLinkedQueue<>();

        int commonReaders = 120;
        int criticalReaders = 50;
        int writers = 30;
        int totalThreads = commonReaders + criticalReaders + writers;
        int iterations = 3;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CyclicBarrier startGate = new CyclicBarrier(totalThreads);
        CountDownLatch done = new CountDownLatch(totalThreads);

        for (int i = 1; i <= commonReaders; i++) {
            submitActor(executor, coordinator, probe, violations, done, startGate,
                    process(i, AccessRole.COMMON_READER), iterations);
        }
        for (int i = 1; i <= criticalReaders; i++) {
            submitActor(executor, coordinator, probe, violations, done, startGate,
                    process(1_000 + i, AccessRole.CRITICAL_READER), iterations);
        }
        for (int i = 1; i <= writers; i++) {
            submitActor(executor, coordinator, probe, violations, done, startGate,
                    process(2_000 + i, AccessRole.WRITER), iterations);
        }

        check(done.await(15, TimeUnit.SECONDS),
                "Deadlock ou lentidao extrema. Snapshot: " + coordinator.snapshot().compact());
        executor.shutdownNow();

        assertNoViolations(violations, observer);
        check(coordinator.snapshot().activeReaders() == 0, "Leitores ativos restantes apos stress.");
        check(!coordinator.snapshot().writerActive(), "Escritor ativo restante apos stress.");
    }

    private static void writerWaitingClosesTurnstileForLateCommonReaders() throws Exception {
        RecordingObserver observer = new RecordingObserver();
        EventBus eventBus = new EventBus();
        eventBus.addObserver(observer);
        ArcaneSynchronizationCoordinator coordinator =
                new ArcaneSynchronizationCoordinator(VIP_LIMIT, eventBus);

        CountDownLatch readerInside = new CountDownLatch(1);
        CountDownLatch releaseReader = new CountDownLatch(1);
        AtomicBoolean writerEntered = new AtomicBoolean(false);
        AtomicInteger lateReadersBeforeWriter = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();

        Thread holder = thread("holder-reader", () -> {
            try (AccessPermit ignored = coordinator.acquire(process(1, AccessRole.COMMON_READER))) {
                readerInside.countDown();
                await(releaseReader);
            }
        });
        holder.start();
        threads.add(holder);

        check(readerInside.await(2, TimeUnit.SECONDS), "Leitor inicial nao entrou.");

        Thread writerThread = thread("waiting-writer", () -> {
            try (AccessPermit ignored = coordinator.acquire(process(2, AccessRole.WRITER))) {
                writerEntered.set(true);
                sleepMillis(30);
            }
        });
        writerThread.start();
        threads.add(writerThread);

        check(observer.awaitSnapshot(snapshot -> snapshot.waitingWriters() == 1, Duration.ofSeconds(2)),
                "Escritor nao ficou aguardando.");

        for (int i = 0; i < 12; i++) {
            ProcessDescriptor lateReader = process(100 + i, AccessRole.COMMON_READER);
            Thread reader = thread("late-reader-" + i, () -> {
                try (AccessPermit ignored = coordinator.acquire(lateReader)) {
                    if (!writerEntered.get()) {
                        lateReadersBeforeWriter.incrementAndGet();
                    }
                }
            });
            reader.start();
            threads.add(reader);
        }

        sleepMillis(80);
        check(lateReadersBeforeWriter.get() == 0, "Leitores comuns furaram a catraca.");

        releaseReader.countDown();
        joinAll(threads);

        check(writerEntered.get(), "Escritor deveria ter sido atendido.");
        check(lateReadersBeforeWriter.get() == 0, "Leitores comuns entraram antes do escritor.");
        assertNoSnapshotViolations(observer);
    }

    private static void commonReadersWaitingBehindWriterReceiveBoundedTurnAfterWriter() throws Exception {
        RecordingObserver observer = new RecordingObserver();
        EventBus eventBus = new EventBus();
        eventBus.addObserver(observer);
        ArcaneSynchronizationCoordinator coordinator =
                new ArcaneSynchronizationCoordinator(VIP_LIMIT, eventBus);

        CountDownLatch firstWriterInside = new CountDownLatch(1);
        CountDownLatch releaseFirstWriter = new CountDownLatch(1);
        AtomicBoolean secondWriterEntered = new AtomicBoolean(false);
        AtomicInteger commonReadersBeforeSecondWriter = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();

        Thread writerHolder = thread("writer-holder", () -> {
            try (AccessPermit ignored = coordinator.acquire(process(1, AccessRole.WRITER))) {
                firstWriterInside.countDown();
                await(releaseFirstWriter);
            }
        });
        writerHolder.start();
        threads.add(writerHolder);

        check(firstWriterInside.await(2, TimeUnit.SECONDS), "Primeiro escritor nao entrou.");

        Thread queuedWriter = thread("queued-writer", () -> {
            try (AccessPermit ignored = coordinator.acquire(process(2, AccessRole.WRITER))) {
                secondWriterEntered.set(true);
                sleepMillis(25);
            }
        });
        queuedWriter.start();
        threads.add(queuedWriter);

        check(observer.awaitSnapshot(snapshot -> snapshot.waitingWriters() == 1, Duration.ofSeconds(2)),
                "Segundo escritor nao entrou na fila.");

        int waitingCommonReaders = 10;
        for (int i = 0; i < waitingCommonReaders; i++) {
            ProcessDescriptor reader = process(100 + i, AccessRole.COMMON_READER);
            Thread commonReader = thread("common-after-writer-" + i, () -> {
                try (AccessPermit ignored = coordinator.acquire(reader)) {
                    if (!secondWriterEntered.get()) {
                        commonReadersBeforeSecondWriter.incrementAndGet();
                    }
                    sleepMillis(5);
                }
            });
            commonReader.start();
            threads.add(commonReader);
        }

        check(observer.awaitSnapshot(snapshot -> snapshot.waitingCommonReaders() == waitingCommonReaders,
                        Duration.ofSeconds(2)),
                "Leitores comuns nao ficaram aguardando atras do escritor ativo.");

        releaseFirstWriter.countDown();
        joinAll(threads);

        check(secondWriterEntered.get(), "Segundo escritor deveria ser atendido apos o lote.");
        check(commonReadersBeforeSecondWriter.get() == waitingCommonReaders,
                "Leitores comuns aguardando deveriam receber lote justo.");
        assertNoSnapshotViolations(observer);
    }

    private static void criticalReaderVipLimitForcesWriterBeforeNextCriticalReader() throws Exception {
        RecordingObserver observer = new RecordingObserver();
        EventBus eventBus = new EventBus();
        eventBus.addObserver(observer);
        ArcaneSynchronizationCoordinator coordinator = new ArcaneSynchronizationCoordinator(1, eventBus);

        CountDownLatch firstReaderInside = new CountDownLatch(1);
        CountDownLatch releaseReaders = new CountDownLatch(1);
        Queue<AccessRole> entryOrder = new ConcurrentLinkedQueue<>();
        List<Thread> threads = new ArrayList<>();

        Thread commonReader = thread("reader-holder", () -> {
            try (AccessPermit ignored = coordinator.acquire(process(1, AccessRole.COMMON_READER))) {
                firstReaderInside.countDown();
                await(releaseReaders);
            }
        });
        commonReader.start();
        threads.add(commonReader);

        check(firstReaderInside.await(2, TimeUnit.SECONDS), "Leitor inicial nao entrou.");

        Thread writer = thread("writer", () -> {
            try (AccessPermit ignored = coordinator.acquire(process(2, AccessRole.WRITER))) {
                entryOrder.add(AccessRole.WRITER);
                sleepMillis(20);
            }
        });
        writer.start();
        threads.add(writer);

        check(observer.awaitSnapshot(snapshot -> snapshot.waitingWriters() == 1, Duration.ofSeconds(2)),
                "Escritor nao ficou aguardando.");

        CountDownLatch firstCriticalMayExit = new CountDownLatch(1);
        Thread criticalA = thread("critical-a", () -> {
            try (AccessPermit ignored = coordinator.acquire(process(3, AccessRole.CRITICAL_READER))) {
                entryOrder.add(AccessRole.CRITICAL_READER);
                await(firstCriticalMayExit);
            }
        });
        Thread criticalB = thread("critical-b", () -> {
            try (AccessPermit ignored = coordinator.acquire(process(4, AccessRole.CRITICAL_READER))) {
                entryOrder.add(AccessRole.CRITICAL_READER);
            }
        });
        criticalA.start();
        criticalB.start();
        threads.add(criticalA);
        threads.add(criticalB);

        check(observer.awaitSnapshot(snapshot -> snapshot.criticalVipBurst() == 1
                        && snapshot.waitingCriticalReaders() == 1,
                Duration.ofSeconds(2)), "Limite VIP nao foi atingido com um critico bloqueado.");

        releaseReaders.countDown();
        sleepMillis(50);
        check(!entryOrder.contains(AccessRole.WRITER),
                "Escritor nao pode entrar enquanto o primeiro leitor critico esta ativo.");

        firstCriticalMayExit.countDown();
        joinAll(threads);

        List<AccessRole> order = new ArrayList<>(entryOrder);
        check(order.equals(List.of(AccessRole.CRITICAL_READER, AccessRole.WRITER, AccessRole.CRITICAL_READER)),
                "Ordem esperada com limite VIP=1: critico, escritor, critico. Obtida: " + order);
        assertNoSnapshotViolations(observer);
    }

    private static void waitingCountersRecoverAfterInterruptions() throws Exception {
        RecordingObserver observer = new RecordingObserver();
        EventBus eventBus = new EventBus();
        eventBus.addObserver(observer);
        ArcaneSynchronizationCoordinator coordinator =
                new ArcaneSynchronizationCoordinator(VIP_LIMIT, eventBus);

        CountDownLatch writerInside = new CountDownLatch(1);
        CountDownLatch releaseWriter = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();

        Thread writerHolder = thread("writer-holder", () -> {
            try (AccessPermit ignored = coordinator.acquire(process(1, AccessRole.WRITER))) {
                writerInside.countDown();
                await(releaseWriter);
            }
        });
        writerHolder.start();
        threads.add(writerHolder);

        check(writerInside.await(2, TimeUnit.SECONDS), "Escritor inicial nao entrou.");

        for (int i = 0; i < 30; i++) {
            AccessRole role = switch (i % 3) {
                case 0 -> AccessRole.COMMON_READER;
                case 1 -> AccessRole.CRITICAL_READER;
                default -> AccessRole.WRITER;
            };
            ProcessDescriptor descriptor = process(100 + i, role);
            Thread waiter = thread("interrupted-waiter-" + i, () -> {
                try (AccessPermit ignored = coordinator.acquire(descriptor)) {
                    sleepMillis(10);
                }
            });
            waiter.start();
            threads.add(waiter);
        }

        check(observer.awaitSnapshot(snapshot ->
                        snapshot.waitingCommonReaders() > 0
                                && snapshot.waitingCriticalReaders() > 0
                                && snapshot.waitingWriters() > 0,
                Duration.ofSeconds(2)), "Threads aguardando nao foram registradas.");

        threads.stream()
                .filter(thread -> !thread.getName().equals("writer-holder"))
                .forEach(Thread::interrupt);

        check(observer.awaitSnapshot(snapshot ->
                        snapshot.waitingCommonReaders() == 0
                                && snapshot.waitingCriticalReaders() == 0
                                && snapshot.waitingWriters() == 0,
                Duration.ofSeconds(2)), "Contadores de espera nao zeraram apos interrupcoes.");

        releaseWriter.countDown();
        joinAll(threads);

        SynchronizationSnapshot snapshot = coordinator.snapshot();
        check(snapshot.activeReaders() == 0, "Leitores ativos restantes.");
        check(!snapshot.writerActive(), "Escritor ativo restante.");
        check(snapshot.waitingCommonReaders() == 0, "Leitores comuns aguardando restantes.");
        check(snapshot.waitingCriticalReaders() == 0, "Leitores criticos aguardando restantes.");
        check(snapshot.waitingWriters() == 0, "Escritores aguardando restantes.");
        assertNoSnapshotViolations(observer);
    }

    private static void commonReaderBatchQuotaDoesNotSurviveInterruptedReaders() throws Exception {
        RecordingObserver observer = new RecordingObserver();
        EventBus eventBus = new EventBus();
        eventBus.addObserver(observer);
        ArcaneSynchronizationCoordinator coordinator =
                new ArcaneSynchronizationCoordinator(VIP_LIMIT, eventBus);

        CountDownLatch firstWriterInside = new CountDownLatch(1);
        CountDownLatch releaseFirstWriter = new CountDownLatch(1);
        AtomicBoolean secondWriterEntered = new AtomicBoolean(false);
        AtomicInteger readersInterruptedBeforeAccess = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();

        Thread firstWriter = thread("quota-writer-holder", () -> {
            try (AccessPermit ignored = coordinator.acquire(process(1, AccessRole.WRITER))) {
                firstWriterInside.countDown();
                await(releaseFirstWriter);
            }
        });
        firstWriter.start();
        threads.add(firstWriter);

        check(firstWriterInside.await(2, TimeUnit.SECONDS), "Primeiro escritor nao entrou.");

        Thread secondWriter = thread("quota-second-writer", () -> {
            try (AccessPermit ignored = coordinator.acquire(process(2, AccessRole.WRITER))) {
                secondWriterEntered.set(true);
            }
        });
        secondWriter.start();
        threads.add(secondWriter);

        check(observer.awaitSnapshot(snapshot -> snapshot.waitingWriters() == 1, Duration.ofSeconds(2)),
                "Segundo escritor nao ficou aguardando.");

        int readers = 220;
        List<Thread> interruptibleReaders = new ArrayList<>();
        CountDownLatch readersStarted = new CountDownLatch(readers);
        for (int i = 0; i < readers; i++) {
            ProcessDescriptor reader = process(1_000 + i, AccessRole.COMMON_READER);
            Thread thread = new Thread(() -> {
                readersStarted.countDown();
                try (AccessPermit ignored = coordinator.acquire(reader)) {
                    sleepMillis(5);
                } catch (InterruptedException interrupted) {
                    readersInterruptedBeforeAccess.incrementAndGet();
                    Thread.currentThread().interrupt();
                }
            }, "quota-reader-" + i);
            thread.start();
            threads.add(thread);
            interruptibleReaders.add(thread);
        }

        check(readersStarted.await(2, TimeUnit.SECONDS), "Leitores de quota nao iniciaram.");
        check(observer.awaitSnapshot(snapshot -> snapshot.waitingCommonReaders() > 0, Duration.ofSeconds(2)),
                "Leitores comuns nao ficaram aguardando.");

        releaseFirstWriter.countDown();
        check(observer.awaitSnapshot(snapshot -> snapshot.commonReaderBatchQuota() > 0,
                Duration.ofSeconds(2)), "Quota de leitores comuns nao foi criada apos escrita.");
        interruptibleReaders.forEach(Thread::interrupt);

        joinAll(threads);

        check(readersInterruptedBeforeAccess.get() > 0,
                "O teste precisa interromper pelo menos um leitor antes do acesso.");
        check(secondWriterEntered.get(), "Segundo escritor nao pode ficar preso por quota fantasma.");
        check(coordinator.snapshot().commonReaderBatchQuota() == 0,
                "Quota de lote comum deveria zerar apos interrupcoes e progresso.");
        assertNoSnapshotViolations(observer);
    }

    private static void submitActor(
            ExecutorService executor,
            ArcaneSynchronizationCoordinator coordinator,
            CriticalRegionProbe probe,
            Queue<String> violations,
            CountDownLatch done,
            CyclicBarrier startGate,
            ProcessDescriptor process,
            int iterations
    ) {
        executor.submit(() -> {
            try {
                startGate.await();
                for (int i = 0; i < iterations; i++) {
                    try (AccessPermit ignored = coordinator.acquire(process)) {
                        if (process.role().isWriter()) {
                            probe.enterWriter(process, violations);
                            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
                            probe.exitWriter();
                        } else {
                            probe.enterReader(process, violations);
                            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
                            probe.exitReader();
                        }
                    }
                }
            } catch (Exception exception) {
                violations.add(process.label() + " falhou: " + exception);
            } finally {
                done.countDown();
            }
        });
    }

    private static void run(String name, int repetitions, InterruptibleRunnable runnable, List<Failure> failures) {
        for (int repetition = 1; repetition <= repetitions; repetition++) {
            String displayName = name + "[" + repetition + "/" + repetitions + "]";
            try {
                runnable.run();
                System.out.println("[PASS] " + displayName);
            } catch (Throwable throwable) {
                failures.add(new Failure(displayName, throwable));
                System.err.println("[FAIL] " + displayName);
            }
        }
    }

    private static ProcessDescriptor process(int id, AccessRole role) {
        return new ProcessDescriptor(id, "P" + id, role);
    }

    private static Thread thread(String name, InterruptibleRunnable runnable) {
        return new Thread(() -> {
            try {
                runnable.run();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }, name);
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        latch.await();
    }

    private static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static void joinAll(List<Thread> threads) throws InterruptedException {
        for (Thread thread : threads) {
            thread.join(2_000);
            check(!thread.isAlive(), "Thread nao finalizou: " + thread.getName());
        }
    }

    private static void assertNoViolations(Queue<String> violations, RecordingObserver observer) {
        check(violations.isEmpty(), String.join(System.lineSeparator(), violations));
        assertNoSnapshotViolations(observer);
    }

    private static void assertNoSnapshotViolations(RecordingObserver observer) {
        List<String> violations = observer.snapshotViolations();
        check(violations.isEmpty(), String.join(System.lineSeparator(), violations));
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    private interface InterruptibleRunnable {
        void run() throws Exception;
    }

    private record Failure(String name, Throwable cause) {
    }

    private static final class CriticalRegionProbe {
        private final AtomicInteger readersInside = new AtomicInteger();
        private final AtomicInteger writersInside = new AtomicInteger();

        void enterReader(ProcessDescriptor process, Queue<String> violations) {
            if (writersInside.get() > 0) {
                violations.add(process.label() + " entrou lendo com escritor ativo.");
            }
            int readers = readersInside.incrementAndGet();
            if (readers < 1) {
                violations.add("Contador de leitores invalido ao entrar: " + readers);
            }
            if (writersInside.get() > 0) {
                violations.add(process.label() + " detectou escritor apos entrar lendo.");
            }
        }

        void exitReader() {
            int readers = readersInside.decrementAndGet();
            if (readers < 0) {
                readersInside.incrementAndGet();
            }
        }

        void enterWriter(ProcessDescriptor process, Queue<String> violations) {
            int writers = writersInside.incrementAndGet();
            if (writers > 1) {
                violations.add(process.label() + " entrou com outro escritor ativo.");
            }
            int readers = readersInside.get();
            if (readers > 0) {
                violations.add(process.label() + " entrou escrevendo com " + readers + " leitores ativos.");
            }
        }

        void exitWriter() {
            int writers = writersInside.decrementAndGet();
            if (writers < 0) {
                writersInside.incrementAndGet();
            }
        }
    }

    private static final class RecordingObserver implements SimulationObserver {
        private final Queue<SimulationEvent> events = new ConcurrentLinkedQueue<>();

        @Override
        public void onEvent(SimulationEvent event) {
            events.add(event);
        }

        boolean awaitSnapshot(Predicate<SynchronizationSnapshot> predicate, Duration timeout)
                throws InterruptedException {
            long deadline = System.nanoTime() + timeout.toNanos();
            while (System.nanoTime() < deadline) {
                if (events.stream()
                        .map(SimulationEvent::snapshot)
                        .anyMatch(snapshot -> snapshot != null && predicate.test(snapshot))) {
                    return true;
                }
                Thread.sleep(10);
            }
            return false;
        }

        List<String> snapshotViolations() {
            return events.stream()
                    .sorted(Comparator.comparing(SimulationEvent::timestamp))
                    .map(this::snapshotViolation)
                    .filter(violation -> !violation.isBlank())
                    .toList();
        }

        private String snapshotViolation(SimulationEvent event) {
            SynchronizationSnapshot snapshot = event.snapshot();
            if (snapshot == null) {
                return "";
            }
            if (snapshot.activeReaders() < 0
                    || snapshot.waitingCommonReaders() < 0
                    || snapshot.waitingCriticalReaders() < 0
                    || snapshot.waitingWriters() < 0
                    || snapshot.commonReaderBatchQuota() < 0
                    || snapshot.criticalVipBurst() < 0) {
                return "Contador negativo em " + event + " -> " + snapshot.compact();
            }
            if (snapshot.writerActive() && snapshot.activeReaders() > 0) {
                return "Leitor e escritor simultaneos em " + event + " -> " + snapshot.compact();
            }
            if (snapshot.criticalVipBurst() > snapshot.maxCriticalVipBurst()) {
                return "Limite VIP excedido em " + event + " -> " + snapshot.compact();
            }
            if (snapshot.commonReaderBatchQuota() > snapshot.waitingCommonReaders()) {
                return "Quota de leitores comuns maior que fila real em " + event
                        + " -> " + snapshot.compact();
            }
            return "";
        }
    }
}
