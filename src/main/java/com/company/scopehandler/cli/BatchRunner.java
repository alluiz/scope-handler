package com.company.scopehandler.cli;

import com.company.scopehandler.api.services.AuditService;
import com.company.scopehandler.api.services.BatchExecutorService;
import com.company.scopehandler.api.services.BatchPlannerService;
import com.company.scopehandler.api.services.BatchReport;
import com.company.scopehandler.api.services.ReportService;
import com.company.scopehandler.api.usecases.ExecuteBatchUseCase;

import java.nio.file.Path;
import java.util.Locale;

public final class BatchRunner {
    public void run(BatchRunInput input) {
        BatchPlannerService plannerService = new BatchPlannerService();
        BatchExecutorService executorService = new BatchExecutorService();
        ExecuteBatchUseCase useCase = new ExecuteBatchUseCase(plannerService, executorService);

        Path cacheDir = input.auditDir().resolve("cache");
        Path cacheFile = cacheDir.resolve("resume-cache-" + input.asName() + "-" + input.environment() + ".txt");
        com.company.scopehandler.api.cache.ExecutionCache cache = com.company.scopehandler.api.cache.ExecutionCache.load(cacheFile, !input.ignoreCache());

        boolean completed = false;
        try (AuditService auditService = new AuditService(input.auditDir());
             com.company.scopehandler.api.cache.ExecutionCache ignored = cache) {
            BatchReport report = useCase.execute(
                    input.clients(),
                    input.scopes(),
                    input.strategy(),
                    auditService,
                    input.threshold(),
                    input.threads(),
                    input.debug(),
                    cache
            );
            Path reportPath = new ReportService().writeReport(input.auditDir(), report);

            printSummary(report, auditService.getFilePath(), reportPath, input.threshold(), input.threads());
            completed = true;
        } finally {
            if (completed && !input.ignoreCache()) {
                cache.deleteFile();
            }
        }
    }

    private void printSummary(BatchReport report, Path auditFile, Path reportFile, int threshold, int threads) {
        System.out.println("Batch concluido");
        System.out.println("Total: " + report.getTotal());
        System.out.println("Success: " + report.getSuccessCount());
        System.out.println("Failure: " + report.getFailureCount());
        System.out.println("Skipped: " + report.getSkipCount());
        System.out.println("Duracao total: " + com.company.scopehandler.api.services.DurationFormatter.formatSeconds(report.getDurationSeconds()));
        System.out.println("Media por operacao: " + String.format(Locale.ROOT, "%.2f", report.getAverageMsPerOperation()) + "ms");
        System.out.println("Audit: " + auditFile);
        System.out.println("Report: " + reportFile);
        System.out.println("Multi-thread: threshold=" + threshold + " maxThreads=" + threads);
        if (!report.getSampleErrors().isEmpty()) {
            System.out.println("Amostra de erros:");
            for (String err : report.getSampleErrors()) {
                System.out.println("- " + err);
            }
        }
    }
}
