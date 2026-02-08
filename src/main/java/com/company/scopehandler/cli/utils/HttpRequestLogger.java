package com.company.scopehandler.cli.utils;

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
    private final Appender appender;
    private final Path file;

    public HttpRequestLogger(Path file) {
        this.file = file;
        this.logger = createLogger(file);
        this.appender = findAppender();
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
                response.rawStatusCode(), snippet, formatContext(context));
    }

    public void logResponse(ClientRequest request, ClientResponse response, String body, Map<String, String> context) {
        String snippet = body == null ? "" : body.length() > 200 ? body.substring(0, 200) + "..." : body;
        logger.info("RESPONSE {} {} {} status={} body={} ctx={}", Instant.now(), request.method(), request.url(),
                response.rawStatusCode(), snippet, formatContext(context));
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
        if (appender != null) {
            appender.stop();
        }
    }

    private Logger createLogger(Path file) {
        LoggerContext context = LoggerContext.getContext(false);
        Configuration config = context.getConfiguration();
        String loggerName = "http-request-" + UUID.randomUUID();

        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%m%n")
                .withConfiguration(config)
                .build();

        FileAppender fileAppender = FileAppender.newBuilder()
                .withFileName(file.toAbsolutePath().toString())
                .withName(loggerName + "-appender")
                .withLayout(layout)
                .withAppend(true)
                .withImmediateFlush(true)
                .setConfiguration(config)
                .build();
        fileAppender.start();

        LoggerConfig loggerConfig = LoggerConfig.createLogger(
                false, Level.INFO, loggerName, "false",
                null, null, config, null);
        loggerConfig.addAppender(fileAppender, Level.INFO, null);
        config.addLogger(loggerName, loggerConfig);
        context.updateLoggers();

        return LogManager.getLogger(loggerName);
    }

    private Appender findAppender() {
        if (logger instanceof org.apache.logging.log4j.core.Logger coreLogger) {
            return coreLogger.getAppenders().values().stream().findFirst().orElse(null);
        }
        return null;
    }
}
