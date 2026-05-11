package com.arcane.scriptorium.simulation;

import com.arcane.scriptorium.domain.ProcessDescriptor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class ProcessMetrics {
    private final ProcessDescriptor process;
    private final AtomicInteger accesses;
    private final AtomicLong totalWaitMillis;
    private final AtomicLong maxWaitMillis;

    public ProcessMetrics(ProcessDescriptor process) {
        this.process = process;
        this.accesses = new AtomicInteger();
        this.totalWaitMillis = new AtomicLong();
        this.maxWaitMillis = new AtomicLong();
    }

    private ProcessMetrics(ProcessDescriptor process, int accesses, long totalWaitMillis, long maxWaitMillis) {
        this.process = process;
        this.accesses = new AtomicInteger(accesses);
        this.totalWaitMillis = new AtomicLong(totalWaitMillis);
        this.maxWaitMillis = new AtomicLong(maxWaitMillis);
    }

    public void registerAccess(long waitMillis) {
        accesses.incrementAndGet();
        totalWaitMillis.addAndGet(waitMillis);
        maxWaitMillis.accumulateAndGet(waitMillis, Math::max);
    }

    public ProcessMetrics snapshot() {
        return new ProcessMetrics(process, accesses.get(), totalWaitMillis.get(), maxWaitMillis.get());
    }

    public ProcessDescriptor process() {
        return process;
    }

    public int accesses() {
        return accesses.get();
    }

    public long totalWaitMillis() {
        return totalWaitMillis.get();
    }

    public long maxWaitMillis() {
        return maxWaitMillis.get();
    }

    public long averageWaitMillis() {
        int currentAccesses = accesses.get();
        return currentAccesses == 0 ? 0 : totalWaitMillis.get() / currentAccesses;
    }
}
