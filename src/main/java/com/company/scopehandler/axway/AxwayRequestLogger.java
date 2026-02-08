package com.company.scopehandler.axway;

import org.springframework.http.HttpRequest;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public final class AxwayRequestLogger implements AutoCloseable {
    private final BufferedWriter writer;
    private final Path file;

    public AxwayRequestLogger(Path file) {
        try {
            Files.createDirectories(file.getParent());
            this.file = file;
            this.writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open Axway log file", e);
        }
    }

    public void logRequest(ClientRequest request) {
        writeLine("REQUEST " + Instant.now() + " " + request.method() + " " + request.url());
    }

    public void logResponse(HttpRequest request, ClientResponse response, String body) {
        String snippet = body == null ? "" : body.length() > 200 ? body.substring(0, 200) + "..." : body;
        writeLine("RESPONSE " + Instant.now() + " " + request.getMethod() + " " + request.getURI()
                + " status=" + response.rawStatusCode() + " body=" + snippet);
    }

    public void logResponse(ClientRequest request, ClientResponse response, String body) {
        String snippet = body == null ? "" : body.length() > 200 ? body.substring(0, 200) + "..." : body;
        writeLine("RESPONSE " + Instant.now() + " " + request.method() + " " + request.url()
                + " status=" + response.rawStatusCode() + " body=" + snippet);
    }

    private synchronized void writeLine(String line) {
        try {
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write Axway log", e);
        }
    }

    public Path getFile() {
        return file;
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to close Axway log", e);
        }
    }
}
