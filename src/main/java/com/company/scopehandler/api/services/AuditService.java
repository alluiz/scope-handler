package com.company.scopehandler.api.services;

import com.company.scopehandler.api.domain.OperationResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;

public final class AuditService implements AutoCloseable {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
            .withZone(ZoneOffset.UTC);
    private final BufferedWriter writer;
    private final ReentrantLock lock = new ReentrantLock();
    private final Path filePath;

    public AuditService(Path auditDir) {
        try {
            Files.createDirectories(auditDir);
            String fileName = "audit-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(Instant.now().atZone(ZoneOffset.UTC)) + ".csv";
            this.filePath = auditDir.resolve(fileName);
            this.writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            writer.write("timestamp,operationId,sequence,threadIndex,mode,clientId,scope,status,durationMs,thread,message");
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize audit file", e);
        }
    }

    public void record(OperationResult result) {
        lock.lock();
        try {
            writer.write(toCsvLine(result));
            writer.newLine();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write audit line", e);
        } finally {
            lock.unlock();
        }
    }

    public Path getFilePath() {
        return filePath;
    }

    @Override
    public void close() {
        lock.lock();
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to close audit file", e);
        } finally {
            lock.unlock();
        }
    }

    private String toCsvLine(OperationResult result) {
        String timestamp = TS.format(Instant.ofEpochMilli(result.getStartedAtEpochMs()));
        return String.join(",",
                escape(timestamp),
                escape(result.getOperationId()),
                escape(Long.toString(result.getSequence())),
                escape(Long.toString(result.getThreadIndex())),
                escape(result.getMode().name()),
                escape(result.getClientId()),
                escape(result.getScope()),
                escape(result.getStatus().name()),
                escape(Long.toString(result.getDurationMs())),
                escape(result.getThreadName()),
                escape(result.getMessage())
        );
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
