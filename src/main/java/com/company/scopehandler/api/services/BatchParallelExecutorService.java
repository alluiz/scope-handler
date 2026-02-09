package com.company.scopehandler.api.services;

import com.company.scopehandler.api.cache.ExecutionCache;
import com.company.scopehandler.api.domain.Operation;
import com.company.scopehandler.api.domain.OperationResult;
import com.company.scopehandler.api.strategy.ModeStrategy;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

public final class BatchParallelExecutorService {
    private final TaskExecutorService taskExecutor;

    public BatchParallelExecutorService(TaskExecutorService taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public void execute(BatchPlan plan,
                        ModeStrategy strategy,
                        BatchResultHandler handler,
                        BatchExecutionLogger logger,
                        int maxThreads,
                        ExecutionCache cache,
                        long startNano,
                        BatchExecutionState state) {
        AtomicLong processedCounter = new AtomicLong(0);

        Iterable<Callable<OperationResult>> tasks = buildTasks(plan, strategy, cache, state);
        Consumer<OperationResult> onResult = new ResultConsumer(handler, logger, plan, startNano, state, processedCounter);
        Consumer<Throwable> onError = new ErrorConsumer(handler, logger, plan, startNano, state, processedCounter, strategy);

        taskExecutor.execute(tasks, maxThreads, onResult, onError);

        long processed = processedCounter.get();
        logger.logProgress(processed, plan.getTotalOperations(), startNano, true, state);
    }

    private Iterable<Callable<OperationResult>> buildTasks(BatchPlan plan,
                                                           ModeStrategy strategy,
                                                           ExecutionCache cache,
                                                           BatchExecutionState state) {
        return new TaskIterable<>(plan.getOperations(), new TaskFactory(cache, strategy, state));
    }

    private static final class TaskFactory implements Function<Operation, Callable<OperationResult>> {
        private final ExecutionCache cache;
        private final ModeStrategy strategy;
        private final BatchExecutionState state;

        private TaskFactory(ExecutionCache cache, ModeStrategy strategy, BatchExecutionState state) {
            this.cache = cache;
            this.strategy = strategy;
            this.state = state;
        }

        @Override
        public Callable<OperationResult> apply(Operation operation) {
            return new TaskCallable(cache, strategy, operation, state);
        }
    }

    private static final class TaskCallable implements Callable<OperationResult> {
        private final ExecutionCache cache;
        private final ModeStrategy strategy;
        private final Operation operation;
        private final BatchExecutionState state;

        private TaskCallable(ExecutionCache cache,
                             ModeStrategy strategy,
                             Operation operation,
                             BatchExecutionState state) {
            this.cache = cache;
            this.strategy = strategy;
            this.operation = operation;
            this.state = state;
        }

        @Override
        public OperationResult call() {
            return BatchExecutionSupport.maybeSkip(cache, strategy, operation, state);
        }
    }

    private static final class ResultConsumer implements Consumer<OperationResult> {
        private final BatchResultHandler handler;
        private final BatchExecutionLogger logger;
        private final BatchPlan plan;
        private final long startNano;
        private final BatchExecutionState state;
        private final AtomicLong processedCounter;

        private ResultConsumer(BatchResultHandler handler,
                               BatchExecutionLogger logger,
                               BatchPlan plan,
                               long startNano,
                               BatchExecutionState state,
                               AtomicLong processedCounter) {
            this.handler = handler;
            this.logger = logger;
            this.plan = plan;
            this.startNano = startNano;
            this.state = state;
            this.processedCounter = processedCounter;
        }

        @Override
        public void accept(OperationResult result) {
            handler.handle(result);
            long current = processedCounter.incrementAndGet();
            logger.logProgress(current, plan.getTotalOperations(), startNano, false, state);
        }
    }

    private static final class ErrorConsumer implements Consumer<Throwable> {
        private final BatchResultHandler handler;
        private final BatchExecutionLogger logger;
        private final BatchPlan plan;
        private final long startNano;
        private final BatchExecutionState state;
        private final AtomicLong processedCounter;
        private final ModeStrategy strategy;

        private ErrorConsumer(BatchResultHandler handler,
                              BatchExecutionLogger logger,
                              BatchPlan plan,
                              long startNano,
                              BatchExecutionState state,
                              AtomicLong processedCounter,
                              ModeStrategy strategy) {
            this.handler = handler;
            this.logger = logger;
            this.plan = plan;
            this.startNano = startNano;
            this.state = state;
            this.processedCounter = processedCounter;
            this.strategy = strategy;
        }

        @Override
        public void accept(Throwable error) {
            OperationResult result;
            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                result = OperationResultFactory.failure("execution interrupted", strategy.getMode());
            } else {
                result = OperationResultFactory.failure("execution failure: " + error.getMessage(), strategy.getMode());
            }
            handler.handle(result);
            long current = processedCounter.incrementAndGet();
            logger.logProgress(current, plan.getTotalOperations(), startNano, false, state);
        }
    }
}
