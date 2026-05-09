package com.arcane.scriptorium.synchronization;

public record SynchronizationSnapshot(
        int activeReaders,
        boolean writerActive,
        int waitingCommonReaders,
        int waitingCriticalReaders,
        int waitingWriters,
        int commonReaderBatchQuota,
        int criticalVipBurst,
        int maxCriticalVipBurst,
        long completedReads,
        long completedWrites
) {
    public String compact() {
        return "ativos[L=%d,E=%s] fila[LR=%d,LC=%d,ES=%d] loteLR=%d vip=%d/%d concluidos[L=%d,E=%d]"
                .formatted(
                        activeReaders,
                        writerActive ? "sim" : "nao",
                        waitingCommonReaders,
                        waitingCriticalReaders,
                        waitingWriters,
                        commonReaderBatchQuota,
                        criticalVipBurst,
                        maxCriticalVipBurst,
                        completedReads,
                        completedWrites
                );
    }
}
