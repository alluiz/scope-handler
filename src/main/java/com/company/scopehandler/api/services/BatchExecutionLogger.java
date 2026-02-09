package com.company.scopehandler.api.services;

import com.company.scopehandler.api.domain.Mode;
import com.company.scopehandler.api.domain.OperationResult;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class BatchExecutionLogger {
    private final AtomicLong lastLogMs = new AtomicLong(0);
    private final boolean debugEnabled;

    public BatchExecutionLogger(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public void logProgress(long processed, long total, long startNano, boolean finalLog, BatchExecutionState state) {
        long elapsedMs = (System.nanoTime() - startNano) / 1_000_000;
        long last = lastLogMs.get();
        boolean timeToLog = elapsedMs - last >= 5000;
        boolean stepLog = processed % 10 == 0 || processed == total;
        if (!finalLog && !timeToLog && !stepLog) {
            return;
        }
        lastLogMs.set(elapsedMs);
        double avgMs = processed > 0 ? (double) elapsedMs / (double) processed : 0.0;
        String threadName = state.getLastThreadName();
        Map<String, AtomicLong> threadOps = state.getThreadOps();
        long threadCount = threadOps.size();
        long threadOpCount = threadName.isBlank() ? 0 : threadOps.getOrDefault(threadName, new AtomicLong(0)).get();
        long elapsedSec = elapsedMs / 1000;
        String tempo = DurationFormatter.formatSeconds(elapsedSec);
        System.out.println("Progresso: " + processed + "/" + total
                + " | ok=" + state.getSuccessCount()
                + " | fail=" + state.getFailureCount()
                + " | skip=" + state.getSkipCount()
                + " | tempo=" + tempo
                + " | media=" + String.format(java.util.Locale.ROOT, "%.2f", avgMs) + "ms/op"
                + " | seq=" + state.getLastSequence()
                + " | thread=" + (threadName.isBlank() ? "-" : threadName)
                + " | threadOps=" + threadOpCount
                + " | threadsAtivas=" + threadCount);
        System.out.flush();
    }

    public void logThreadSummary(BatchExecutionState state) {
        Map<String, AtomicLong> threadOps = state.getThreadOps();
        if (threadOps.isEmpty()) {
            System.out.println("Resumo por thread: nenhum registro");
            System.out.flush();
            return;
        }
        StringBuilder sb = new StringBuilder("Resumo por thread: ");
        threadOps.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sb.append(entry.getKey())
                        .append("=")
                        .append(entry.getValue().get())
                        .append(", "));
        if (sb.length() >= 2) {
            sb.setLength(sb.length() - 2);
        }
        System.out.println(sb);
        System.out.flush();
    }

    public void logOperationStatus(OperationResult result) {
        if (!debugEnabled && result.getMode() != Mode.LIST && result.getMode() != Mode.FIND) {
            return;
        }
        System.out.println("Operacao #" + result.getSequence()
                + " id=" + result.getOperationId()
                + " thread=" + result.getThreadName()
                + " client=" + result.getClientId()
                + " scope=" + result.getScope()
                + " status=" + result.getStatus().name()
                + " durationMs=" + result.getDurationMs()
                + " msg=" + result.getMessage());
    }

    public void logFinalStatus(BatchReport report) {
        System.out.println("Status final: total=" + report.getTotal()
                + " success=" + report.getSuccessCount()
                + " failure=" + report.getFailureCount()
                + " skip=" + report.getSkipCount()
                + " duration=" + DurationFormatter.formatSeconds(report.getDurationSeconds())
                + " avgMsPerOp=" + String.format(java.util.Locale.ROOT, "%.2f", report.getAverageMsPerOperation()));
        System.out.flush();
    }
}
