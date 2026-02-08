package com.company.scopehandler.cli.utils.http;

import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ClientRequest;

import java.util.Collections;
import java.util.Map;

public final class HttpWebClientFactory {
    private HttpWebClientFactory() {
    }

    public static final String CONTEXT_ATTR = "requestContext";

    public static WebClient build(HttpRequestLogger logger) {
        return WebClient.builder()
                .filter(logFilter(logger))
                .build();
    }

    private static ExchangeFilterFunction logFilter(HttpRequestLogger logger) {
        return (request, next) -> {
            Map<String, String> context = extractContext(request);
            logger.logRequest(request, context);
            return next.exchange(request)
                    .doOnNext(response -> logger.logResponse(response.request(), response, "<body>", context))
                    .doOnError(error -> logger.logException(context, error));
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> extractContext(ClientRequest request) {
        Object value = request.attribute(CONTEXT_ATTR).orElse(Collections.emptyMap());
        if (value instanceof Map<?, ?> map) {
            return (Map<String, String>) map;
        }
        return Collections.emptyMap();
    }
}
