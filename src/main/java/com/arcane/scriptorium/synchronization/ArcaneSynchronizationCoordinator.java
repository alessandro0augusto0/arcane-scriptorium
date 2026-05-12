package com.arcane.scriptorium.synchronization;

import com.arcane.scriptorium.domain.AccessRole;
import com.arcane.scriptorium.domain.ProcessDescriptor;
import com.arcane.scriptorium.domain.ProcessState;
import com.arcane.scriptorium.events.EventBus;
import com.arcane.scriptorium.events.EventType;
import com.arcane.scriptorium.events.SimulationEvent;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Central monitor for the readers-writers policy.
 *
 * <p>
 * All queue counters and priority decisions are guarded by {@code policyMutex}.
 * The semaphore models the critical region itself: one writer owns it alone, or
 * the first reader in a reader batch owns it on behalf of all active readers.
 * </p>
 */
public final class ArcaneSynchronizationCoordinator {
    private final ReentrantLock policyMutex;
    private final Condition stateChanged;
    private final Semaphore criticalRegionGate;
    private final int maxCriticalVipBurst;
    private final EventBus eventBus;

    private volatile boolean starvationPreventionEnabled;

    private int activeReaders;
    private boolean writerActive;
    private int waitingCommonReaders;
    private int waitingCriticalReaders;
    private int waitingWriters;
    // Bounded post-write batch that prevents common readers from starving behind a
    // writer cascade.
    private int commonReaderBatchQuota;
    private int criticalVipBurst;
    private long completedReads;
    private long completedWrites;

    public ArcaneSynchronizationCoordinator(int maxCriticalVipBurst, EventBus eventBus) {
        if (maxCriticalVipBurst < 0) {
            throw new IllegalArgumentException("maxCriticalVipBurst cannot be negative");
        }
        this.maxCriticalVipBurst = maxCriticalVipBurst;
        this.eventBus = eventBus;
        this.policyMutex = new ReentrantLock(true);
        this.stateChanged = policyMutex.newCondition();
        this.criticalRegionGate = new Semaphore(1, true);
        this.starvationPreventionEnabled = true;
    }

    public void setStarvationPreventionEnabled(boolean enabled) {
        policyMutex.lock();
        try {
            boolean wasEnabled = this.starvationPreventionEnabled;
            this.starvationPreventionEnabled = enabled;
            if (!wasEnabled && enabled) {
                stateChanged.signalAll();
            }
        } finally {
            policyMutex.unlock();
        }
    }

    public AccessPermit acquire(ProcessDescriptor process) throws InterruptedException {
        long startedAt = System.nanoTime();
        policyMutex.lockInterruptibly();
        boolean waitingRegistered = false;
        try {
            registerWaiting(process.role());
            waitingRegistered = true;
            publish(EventType.WAITING, process, ProcessState.WAITING, "Entrou na fila de acesso.");

            while (!canEnter(process.role())) {
                publish(EventType.BLOCKED, process, ProcessState.BLOCKED, blockReason(process.role()));
                stateChanged.await();
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("interrupted while waiting for grimoire access");
                }
            }

            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("interrupted before reserving grimoire access");
            }

            unregisterWaiting(process.role());
            waitingRegistered = false;
            reserveCriticalRegion(process);

            long waitedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            publish(EventType.ENTERED, process, stateFor(process.role()), "Acesso concedido apos "
                    + waitedMillis + " ms.");
            return new AccessPermit(this, process, waitedMillis);
        } catch (InterruptedException interrupted) {
            if (waitingRegistered) {
                unregisterInterruptedWaiting(process.role());
                stateChanged.signalAll();
            }
            throw interrupted;
        } catch (RuntimeException runtimeException) {
            if (waitingRegistered) {
                unregisterWaiting(process.role());
                stateChanged.signalAll();
            }
            throw runtimeException;
        } finally {
            policyMutex.unlock();
        }
    }

    public SynchronizationSnapshot snapshot() {
        policyMutex.lock();
        try {
            return snapshotUnsafe();
        } finally {
            policyMutex.unlock();
        }
    }

    void release(ProcessDescriptor process) {
        policyMutex.lock();
        try {
            if (process.role().isReader()) {
                activeReaders -= 1;
                completedReads += 1;
                if (activeReaders == 0) {
                    criticalRegionGate.release();
                }
            } else {
                writerActive = false;
                completedWrites += 1;
                criticalVipBurst = 0;
                commonReaderBatchQuota = waitingCommonReaders;
                criticalRegionGate.release();
            }

            publish(EventType.EXITED, process, ProcessState.RESTING, "Liberou a regiao critica.");
            stateChanged.signalAll();
        } finally {
            policyMutex.unlock();
        }
    }

    private boolean canEnter(AccessRole role) {
        if (writerActive) {
            return false;
        }

        if (!starvationPreventionEnabled) {
            return switch (role) {
                case COMMON_READER, CRITICAL_READER -> true;
                case WRITER -> activeReaders == 0;
            };
        }

        return switch (role) {
            case COMMON_READER -> waitingWriters == 0 || commonReaderBatchQuota > 0;
            case CRITICAL_READER -> !criticalLimitProtectsWriter();
            case WRITER -> activeReaders == 0
                    && commonReaderBatchQuota == 0
                    && (waitingCriticalReaders == 0 || criticalLimitProtectsWriter());
        };
    }

    private void reserveCriticalRegion(ProcessDescriptor process) {
        AccessRole role = process.role();
        if (role.isReader()) {
            if (activeReaders == 0) {
                acquireCriticalRegionGate();
            }
            activeReaders += 1;

            if (role == AccessRole.COMMON_READER && commonReaderBatchQuota > 0) {
                commonReaderBatchQuota -= 1;
                publish(EventType.POLICY, process, ProcessState.READING,
                        "Leitor comum atendido no lote pos-escrita; quota restante="
                                + commonReaderBatchQuota + ".");
            }

            if (role == AccessRole.CRITICAL_READER && waitingWriters > 0) {
                criticalVipBurst += 1;
                publish(EventType.POLICY, process, ProcessState.READING,
                        "Prioridade critica aplicada: " + criticalVipBurst + "/" + maxCriticalVipBurst + ".");
            }
            return;
        }

        acquireCriticalRegionGate();
        writerActive = true;
        publish(EventType.POLICY, process, ProcessState.WRITING,
                "Escritor assumiu exclusividade; privilegio critico sera restaurado ao sair.");
    }

    private void acquireCriticalRegionGate() {
        if (!criticalRegionGate.tryAcquire()) {
            throw new IllegalStateException("critical region gate invariant was violated");
        }
    }

    private boolean criticalLimitProtectsWriter() {
        return waitingWriters > 0 && criticalVipBurst >= maxCriticalVipBurst;
    }

    private String blockReason(AccessRole role) {
        if (writerActive) {
            return "Bloqueado: escritor ativo.";
        }
        if (role == AccessRole.COMMON_READER && waitingWriters > 0) {
            if (commonReaderBatchQuota > 0) {
                return "Aguardando lote de leitores comuns ser liberado.";
            }
            return "Bloqueado pela catraca: escritor aguardando.";
        }
        if (role == AccessRole.CRITICAL_READER && criticalLimitProtectsWriter()) {
            return "Limite critico atingido: escritor deve ser atendido.";
        }
        if (role == AccessRole.WRITER && activeReaders > 0) {
            return "Aguardando leitores ativos sairem.";
        }
        if (role == AccessRole.WRITER && commonReaderBatchQuota > 0) {
            return "Aguardando lote justo de leitores comuns.";
        }
        if (role == AccessRole.WRITER && waitingCriticalReaders > 0) {
            return "Aguardando leitores criticos dentro do limite VIP.";
        }
        return "Aguardando politica de escalonamento.";
    }

    private void registerWaiting(AccessRole role) {
        switch (role) {
            case COMMON_READER -> waitingCommonReaders += 1;
            case CRITICAL_READER -> waitingCriticalReaders += 1;
            case WRITER -> waitingWriters += 1;
        }
    }

    private void unregisterWaiting(AccessRole role) {
        switch (role) {
            case COMMON_READER -> waitingCommonReaders -= 1;
            case CRITICAL_READER -> waitingCriticalReaders -= 1;
            case WRITER -> waitingWriters -= 1;
        }
    }

    private void unregisterInterruptedWaiting(AccessRole role) {
        unregisterWaiting(role);
        if (role == AccessRole.COMMON_READER && commonReaderBatchQuota > waitingCommonReaders) {
            commonReaderBatchQuota = waitingCommonReaders;
        }
    }

    private ProcessState stateFor(AccessRole role) {
        return role.isWriter() ? ProcessState.WRITING : ProcessState.READING;
    }

    private void publish(EventType type, ProcessDescriptor process, ProcessState state, String message) {
        eventBus.publish(SimulationEvent.now(type, process, state, message, snapshotUnsafe()));
    }

    private SynchronizationSnapshot snapshotUnsafe() {
        return new SynchronizationSnapshot(
                activeReaders,
                writerActive,
                waitingCommonReaders,
                waitingCriticalReaders,
                waitingWriters,
                commonReaderBatchQuota,
                criticalVipBurst,
                maxCriticalVipBurst,
                completedReads,
                completedWrites);
    }
}
