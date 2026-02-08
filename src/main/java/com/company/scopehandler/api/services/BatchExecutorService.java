package com.company.scopehandler.api.services;

import com.company.scopehandler.api.domain.Operation;
import com.company.scopehandler.api.domain.OperationResult;
import com.company.scopehandler.api.strategy.ModeStrategy;
import com.company.scopehandler.api.cache.ExecutionCache;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class BatchExecutorService {
    private final AtomicLong sequence = new AtomicLong(0);
    private final ThreadLocal<Long> threadCounter = ThreadLocal.withInitial(() -> 0L);
    private final AtomicReference<String> lastThreadName = new AtomicReference<>("");
    private final AtomicLong lastSequence = new AtomicLong(0);
    private final Map<String, AtomicLong> threadOps = new ConcurrentHashMap<>();
    private final AtomicLong lastLogMs = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong skipCount = new AtomicLong(0);
    private volatile boolean debugEnabled = false;

    public BatchReport execute(BatchPlan plan,
                               ModeStrategy strategy,
                               AuditService auditService,
                               int threshold,
                               int maxThreads,
                               boolean debugEnabled,
                               ExecutionCache cache) {
        sequence.set(0);
        threadCounter.set(0L);
        lastThreadName.set("");
        lastSequence.set(0);
        threadOps.clear();
        lastLogMs.set(0);
        successCount.set(0);
        failureCount.set(0);
        skipCount.set(0);
        this.debugEnabled = debugEnabled;
        long total = plan.getTotalOperations();
        BatchReport report = new BatchReport();
        long startNano = System.nanoTime();

        if (total >= threshold) {
            executeParallel(plan, strategy, auditService, maxThreads, report, cache);
        } else {
            executeSequential(plan, strategy, auditService, report, cache);
        }

        report.finish();
        logThreadSummary();
        logFinalStatus(report);
        return report;
    }

    private void executeSequential(BatchPlan plan,
                                   ModeStrategy strategy,
                                   AuditService auditService,
                                   BatchReport report,
                                   ExecutionCache cache) {
        long startNano = System.nanoTime();
        long processed = 0;
        for (Operation operation : plan.getOperations()) {
            OperationResult result = maybeSkip(cache, strategy, operation);
            updateCounters(result);
            logOperationStatus(result);
            auditService.record(result);
            report.add(result);
            recordCacheIfNeeded(cache, result);
            processed++;
            logProgress(processed, plan.getTotalOperations(), startNano, false);
        }
        logProgress(processed, plan.getTotalOperations(), startNano, true);
    }

    private void executeParallel(BatchPlan plan,
                                 ModeStrategy strategy,
                                 AuditService auditService,
                                 int maxThreads,
                                 BatchReport report,
                                 ExecutionCache cache) {
        int threads = Math.max(1, maxThreads);
        ExecutorService executor = Executors.newFixedThreadPool(threads, new NamedThreadFactory("worker-"));
        CompletionService<OperationResult> completionService = new ExecutorCompletionService<>(executor);
        long submitted = 0;
        long processed = 0;
        long startNano = System.nanoTime();

        try {
            for (Operation operation : plan.getOperations()) {
                completionService.submit(() -> maybeSkip(cache, strategy, operation));
                submitted++;
            }

            for (long i = 0; i < submitted; i++) {
                try {
                    OperationResult result = completionService.take().get();
                    updateCounters(result);
                    logOperationStatus(result);
                    auditService.record(result);
                    report.add(result);
                    recordCacheIfNeeded(cache, result);
                    processed++;
                    logProgress(processed, plan.getTotalOperations(), startNano, false);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    OperationResult result = OperationResultFactory.failure("execution interrupted", strategy.getMode());
                    updateCounters(result);
                    logOperationStatus(result);
                    auditService.record(result);
                    report.add(result);
                    recordCacheIfNeeded(cache, result);
                    processed++;
                    logProgress(processed, plan.getTotalOperations(), startNano, false);
                } catch (ExecutionException e) {
                    OperationResult result = OperationResultFactory.failure("execution failure: " + e.getMessage(), strategy.getMode());
                    updateCounters(result);
                    logOperationStatus(result);
                    auditService.record(result);
                    report.add(result);
                    recordCacheIfNeeded(cache, result);
                    processed++;
                    logProgress(processed, plan.getTotalOperations(), startNano, false);
                }
            }
            logProgress(processed, plan.getTotalOperations(), startNano, true);
        } finally {
            executor.shutdownNow();
        }
    }

    private void logProgress(long processed, long total, long startNano, boolean finalLog) {
        long elapsedMs = (System.nanoTime() - startNano) / 1_000_000;
        long last = lastLogMs.get();
        boolean timeToLog = elapsedMs - last >= 5000;
        boolean stepLog = processed % 10 == 0 || processed == total;
        if (!finalLog && !timeToLog && !stepLog) {
            return;
        }
        lastLogMs.set(elapsedMs);
        double avgMs = processed > 0 ? (double) elapsedMs / (double) processed : 0.0;
        String threadName = lastThreadName.get();
        long threadCount = threadOps.size();
        long threadOpCount = threadName.isBlank() ? 0 : threadOps.getOrDefault(threadName, new AtomicLong(0)).get();
        long elapsedSec = elapsedMs / 1000;
        String tempo = DurationFormatter.formatSeconds(elapsedSec);
        System.out.println("Progresso: " + processed + "/" + total
                + " | ok=" + successCount.get()
                + " | fail=" + failureCount.get()
                + " | skip=" + skipCount.get()
                + " | tempo=" + tempo
                + " | media=" + String.format(java.util.Locale.ROOT, "%.2f", avgMs) + "ms/op"
                + " | seq=" + lastSequence.get()
                + " | thread=" + (threadName.isBlank() ? "-" : threadName)
                + " | threadOps=" + threadOpCount
                + " | threadsAtivas=" + threadCount);
        System.out.flush();
    }

    private void logThreadSummary() {
        if (threadOps.isEmpty()) {
            System.out.println("Resumo por thread: nenhum registro");
            System.out.flush();
            return;
        }
        StringBuilder sb = new StringBuilder("Resumo por thread: ");
        threadOps.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
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

    private void logOperationStatus(OperationResult result) {
        if (!debugEnabled) {
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

    private void updateCounters(OperationResult result) {
        if (result.getStatus() == com.company.scopehandler.api.domain.OperationStatus.OK) {
            successCount.incrementAndGet();
        } else if (result.getStatus() == com.company.scopehandler.api.domain.OperationStatus.FAIL) {
            failureCount.incrementAndGet();
        } else {
            skipCount.incrementAndGet();
        }
    }

    private OperationResult maybeSkip(ExecutionCache cache, ModeStrategy strategy, Operation operation) {
        if (cache != null && cache.isExecuted(strategy.getMode(), operation.getClientId(), operation.getScope())) {
            OperationResult skipped = OperationResultFactory.skipped(
                    strategy.getMode(),
                    operation.getClientId(),
                    operation.getScope(),
                    "skipped (cached)"
            );
            return enrich(skipped, sequence.incrementAndGet());
        }
        return safeExecute(strategy, operation);
    }

    private void recordCacheIfNeeded(ExecutionCache cache, OperationResult result) {
        if (cache == null) {
            return;
        }
        if (result.getStatus() == com.company.scopehandler.api.domain.OperationStatus.OK) {
            cache.record(result.getMode(), result.getClientId(), result.getScope());
        }
    }

    private void logFinalStatus(BatchReport report) {
        System.out.println("Status final: total=" + report.getTotal()
                + " success=" + report.getSuccessCount()
                + " failure=" + report.getFailureCount()
                + " skip=" + report.getSkipCount()
                + " duration=" + DurationFormatter.formatSeconds(report.getDurationSeconds())
                + " avgMsPerOp=" + String.format(java.util.Locale.ROOT, "%.2f", report.getAverageMsPerOperation()));
        System.out.flush();
    }

    private OperationResult safeExecute(ModeStrategy strategy, Operation operation) {
        long opSeq = sequence.incrementAndGet();
        try {
            OperationResult base = strategy.execute(operation);
            return enrich(base, opSeq);
        } catch (Exception e) {
            OperationResult base = OperationResultFactory.failure(
                    "unexpected error: " + e.getMessage(),
                    strategy.getMode(),
                    operation.getClientId(),
                    operation.getScope()
            );
            return enrich(base, opSeq);
        }
    }

    private OperationResult enrich(OperationResult base, long sequence) {
        long threadIndex = threadCounter.get() + 1;
        threadCounter.set(threadIndex);
        String operationId = java.util.UUID.randomUUID().toString();
        String threadName = Thread.currentThread().getName();
        threadOps.computeIfAbsent(threadName, k -> new AtomicLong(0)).incrementAndGet();
        lastThreadName.set(threadName);
        lastSequence.set(sequence);
        return OperationResult.withMeta(base, operationId, sequence, threadIndex);
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(0);

        private NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName(prefix + counter.incrementAndGet());
            return thread;
        }
    }
}
