package com.company.scopehandler.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class ReportService {
    public Path writeReport(Path auditDir, BatchReport report) {
        try {
            Files.createDirectories(auditDir);
            String fileName = "report-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                    .format(report.getFinishedAt().atZone(ZoneOffset.UTC)) + ".txt";
            Path reportPath = auditDir.resolve(fileName);
            String content = buildContent(report);
            Files.writeString(reportPath, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return reportPath;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write report", e);
        }
    }

    private String buildContent(BatchReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Batch report\n");
        sb.append("Started: ").append(report.getStartedAt()).append("\n");
        sb.append("Finished: ").append(report.getFinishedAt()).append("\n");
        sb.append("Total: ").append(report.getTotal()).append("\n");
        sb.append("Success: ").append(report.getSuccessCount()).append("\n");
        sb.append("Failure: ").append(report.getFailureCount()).append("\n");
        sb.append("Skipped: ").append(report.getSkipCount()).append("\n");
        sb.append("Duration: ").append(DurationFormatter.formatSeconds(report.getDurationSeconds())).append("\n");
        sb.append("AvgMsPerOp: ").append(String.format(java.util.Locale.ROOT, "%.2f", report.getAverageMsPerOperation())).append("\n");
        if (!report.getSampleErrors().isEmpty()) {
            sb.append("\nSample errors:\n");
            for (String error : report.getSampleErrors()) {
                sb.append("- ").append(error).append("\n");
            }
        }
        return sb.toString();
    }
}
