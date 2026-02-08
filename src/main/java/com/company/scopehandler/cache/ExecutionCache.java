package com.company.scopehandler.cache;

import com.company.scopehandler.domain.Mode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

public final class ExecutionCache implements AutoCloseable {
    private final Path file;
    private final Set<String> executed;
    private final BufferedWriter writer;

    private ExecutionCache(Path file, Set<String> executed, BufferedWriter writer) {
        this.file = file;
        this.executed = executed;
        this.writer = writer;
    }

    public static ExecutionCache load(Path file, boolean enabled) {
        if (!enabled) {
            return new ExecutionCache(file, new HashSet<>(), null);
        }
        Set<String> executed = new HashSet<>();
        if (Files.exists(file)) {
            try {
                Files.readAllLines(file, StandardCharsets.UTF_8)
                        .forEach(line -> {
                            String trimmed = line.trim();
                            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                                executed.add(trimmed);
                            }
                        });
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read cache: " + file, e);
            }
        }
        try {
            Files.createDirectories(file.getParent());
            BufferedWriter writer = Files.newBufferedWriter(
                    file,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
            return new ExecutionCache(file, executed, writer);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open cache: " + file, e);
        }
    }

    public boolean isExecuted(Mode mode, String clientId, String scope) {
        return executed.contains(key(mode, clientId, scope));
    }

    public void record(Mode mode, String clientId, String scope) {
        if (writer == null) {
            return;
        }
        String key = key(mode, clientId, scope);
        if (executed.add(key)) {
            try {
                writer.write(key);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to write cache", e);
            }
        }
    }

    public Path getFile() {
        return file;
    }

    public void deleteFile() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {
                // best-effort
            }
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete cache file: " + file, e);
        }
    }

    private String key(Mode mode, String clientId, String scope) {
        return mode.name() + "|" + clientId + "|" + scope;
    }

    @Override
    public void close() {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to close cache", e);
        }
    }
}
