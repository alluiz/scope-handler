package com.company.scopehandler.api.services;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class TaskExecutorService {
    public <T> void execute(Iterable<Callable<T>> tasks,
                            int maxThreads,
                            Consumer<T> onResult,
                            Consumer<Throwable> onError) {
        if (tasks == null) {
            return;
        }
        int threads = Math.max(1, maxThreads);
        if (threads <= 1) {
            for (Callable<T> task : tasks) {
                try {
                    T result = task.call();
                    if (result != null && onResult != null) {
                        onResult.accept(result);
                    }
                } catch (Exception e) {
                    if (onError != null) {
                        onError.accept(e);
                    }
                }
            }
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(threads, new NamedThreadFactory("worker-"));
        CompletionService<T> completionService = new ExecutorCompletionService<>(executor);
        int submitted = 0;
        try {
            for (Callable<T> task : tasks) {
                completionService.submit(task);
                submitted++;
            }
            for (int i = 0; i < submitted; i++) {
                try {
                    T result = completionService.take().get();
                    if (result != null && onResult != null) {
                        onResult.accept(result);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (onError != null) {
                        onError.accept(e);
                    }
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    if (onError != null) {
                        onError.accept(cause);
                    }
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
