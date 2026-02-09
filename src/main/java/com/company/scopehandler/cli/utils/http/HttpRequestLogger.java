package com.company.scopehandler.cli.utils.http;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.Level;
import org.springframework.http.HttpRequest;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class HttpRequestLogger implements AutoCloseable {
    private final Logger logger;
    private final Appender fileAppender;
    private final Path file;

    public HttpRequestLogger(Path file) {
        this(file, false);
    }

    public HttpRequestLogger(Path file, boolean debug) {
        this.file = java.util.Objects.requireNonNull(file, "file");
        if (this.file.getParent() != null) {
            try {
                java.nio.file.Files.createDirectories(this.file.getParent());
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Failed to create log directory", e);
            }
        }
        this.logger = createLogger(file, debug);
        this.fileAppender = findAppender(logger, "-file");
    }

    public void logRequest(ClientRequest request, Map<String, String> context) {
        logger.info("REQUEST {} {} {} ctx={}", Instant.now(), request.method(), request.url(), formatContext(context));
    }

    public void logRequest(HttpRequest request, Map<String, String> context) {
        logger.info("REQUEST {} {} {} ctx={}", Instant.now(), request.getMethod(), request.getURI(), formatContext(context));
    }

    public void logResponse(HttpRequest request, ClientResponse response, String body, Map<String, String> context) {
        String snippet = body == null ? "" : body.length() > 200 ? body.substring(0, 200) + "..." : body;
        logger.info("RESPONSE {} {} {} status={} body={} ctx={}", Instant.now(), request.getMethod(), request.getURI(),
                response.statusCode(), snippet, formatContext(context));
    }

    public void logResponse(ClientRequest request, ClientResponse response, String body, Map<String, String> context) {
        String snippet = body == null ? "" : body.length() > 200 ? body.substring(0, 200) + "..." : body;
        logger.info("RESPONSE {} {} {} status={} body={} ctx={}", Instant.now(), request.method(), request.url(),
                response.statusCode(), snippet, formatContext(context));
    }

    public void logException(Map<String, String> context, Throwable error) {
        logger.warn("EXCEPTION {} ctx={} error={} message={}", Instant.now(), formatContext(context),
                error.getClass().getSimpleName(), safeMessage(error));
    }

    private String safeMessage(Throwable error) {
        if (error == null || error.getMessage() == null) {
            return "";
        }
        String msg = error.getMessage();
        return msg.length() > 200 ? msg.substring(0, 200) + "..." : msg;
    }

    private String formatContext(Map<String, String> context) {
        if (context == null || context.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (var entry : context.entrySet()) {
            if (!first) {
                sb.append(' ');
            }
            sb.append(entry.getKey()).append('=').append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }

    public Path getFile() {
        return file;
    }

    @Override
    public void close() {
        if (fileAppender != null) {
            fileAppender.stop();
        }
    }

    private Logger createLogger(Path file, boolean debug) {
        LoggerContext context = LoggerContext.getContext(false);
        Configuration config = context.getConfiguration();
        String loggerName = "http-request-" + UUID.randomUUID();

        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%m%n")
                .withConfiguration(config)
                .build();

        FileAppender fileAppender = FileAppender.newBuilder()
                .setName(loggerName + "-file")
                .setLayout(layout)
                .setImmediateFlush(true)
                .withAppend(true)
                .withFileName(file.toAbsolutePath().toString())
                .setConfiguration(config)
                .build();
                
        fileAppender.start();

        LoggerConfig loggerConfig = new LoggerConfig(loggerName, Level.INFO, false);
        loggerConfig.addAppender(fileAppender, Level.INFO, null);

        if (debug) {
            org.apache.logging.log4j.core.appender.ConsoleAppender consoleAppender =
                    org.apache.logging.log4j.core.appender.ConsoleAppender.newBuilder()
                            .setName(loggerName + "-console")
                            .setLayout(layout)
                            .setTarget(org.apache.logging.log4j.core.appender.ConsoleAppender.Target.SYSTEM_OUT)
                            .setConfiguration(config)
                            .build();
            consoleAppender.start();
            loggerConfig.addAppender(consoleAppender, Level.DEBUG, null);
        }

        config.addLogger(loggerName, loggerConfig);
        context.updateLoggers();

        return LogManager.getLogger(loggerName);
    }

    private Appender findAppender(Logger logger, String suffix) {
        if (logger instanceof org.apache.logging.log4j.core.Logger coreLogger) {
            return coreLogger.getAppenders().values().stream()
                    .filter(app -> app.getName() != null && app.getName().endsWith(suffix))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}
