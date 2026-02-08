package com.company.scopehandler.providers.axway;

import com.company.scopehandler.providers.axway.cache.AxwayCacheStore;
import com.company.scopehandler.api.config.AuthorizationServerSettings;
import com.company.scopehandler.api.domain.OperationOutcome;
import com.company.scopehandler.cli.utils.HttpRequestLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AxwayAuthorizationServerServiceTest {

    private MockWebServer server;

    @BeforeEach
    void setup() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void teardown() throws Exception {
        server.shutdown();
    }

    @Test
    void associateReturnsSkipOn409(@TempDir Path tempDir) {
        server.setDispatcher(new AxwayDispatcher()
                .whenGet("/api/portal/v1.2/applications/oauthclient/client-1", okJson("{\"id\":\"app-1\"}"))
                .whenPost("/api/portal/v1.2/applications/app-1/scope", conflictJson("conflict")));

        AxwayAuthorizationServerClient rpcClient = new AxwayAuthorizationServerClient(
                settings(server.url("/").toString()),
                Duration.ofSeconds(5),
                new HttpRequestLogger(tempDir.resolve("axway.log"))
        );
        AxwayAuthorizationServerService client = new AxwayAuthorizationServerService(
                rpcClient,
                new AxwayCacheStore(tempDir.resolve("axway.json"), new com.fasterxml.jackson.databind.ObjectMapper())
        );

        OperationOutcome outcome = client.associateScope("client-1", "scope-1");

        assertEquals(409, outcome.getStatusCode());
        assertEquals("SKIP", outcome.getStatus().name());

        RecordedRequest req1 = take();
        assertRequest(req1, "GET", "/api/portal/v1.2/applications/oauthclient/client-1");

        RecordedRequest req2 = take();
        assertRequest(req2, "POST", "/api/portal/v1.2/applications/app-1/scope");
        String body = req2.getBody().readUtf8();
        assertJson(body, "applicationId", "app-1");
        assertJson(body, "scope", "scope-1");
    }

    @Test
    void dissociateSkipsWhenScopeMissing(@TempDir Path tempDir) {
        server.setDispatcher(new AxwayDispatcher()
                .whenGet("/api/portal/v1.2/applications/oauthclient/client-1", okJson("{\"id\":\"app-1\"}"))
                .whenGet("/api/portal/v1.2/applications/app-1/scope", okJson("[]")));

        AxwayAuthorizationServerClient rpcClient = new AxwayAuthorizationServerClient(
                settings(server.url("/").toString()),
                Duration.ofSeconds(5),
                new HttpRequestLogger(tempDir.resolve("axway.log"))
        );
        AxwayAuthorizationServerService client = new AxwayAuthorizationServerService(
                rpcClient,
                new AxwayCacheStore(tempDir.resolve("axway.json"), new com.fasterxml.jackson.databind.ObjectMapper())
        );

        OperationOutcome outcome = client.dissociateScope("client-1", "scope-1");

        assertEquals(404, outcome.getStatusCode());
        assertEquals("SKIP", outcome.getStatus().name());

        RecordedRequest req1 = take();
        assertRequest(req1, "GET", "/api/portal/v1.2/applications/oauthclient/client-1");

        RecordedRequest req2 = take();
        assertRequest(req2, "GET", "/api/portal/v1.2/applications/app-1/scope");
    }

    @Test
    void dissociateDeletesWhenScopeFound(@TempDir Path tempDir) {
        server.setDispatcher(new AxwayDispatcher()
                .whenGet("/api/portal/v1.2/applications/oauthclient/client-1", okJson("{\"id\":\"app-1\"}"))
                .whenGet("/api/portal/v1.2/applications/app-1/scope", okJson("[{\"id\":\"scope-1-id\",\"scope\":\"scope-1\"}]"))
                .whenDelete("/api/portal/v1.2/applications/app-1/scope/scope-1-id", okJson("")));

        AxwayAuthorizationServerClient rpcClient = new AxwayAuthorizationServerClient(
                settings(server.url("/").toString()),
                Duration.ofSeconds(5),
                new HttpRequestLogger(tempDir.resolve("axway.log"))
        );
        AxwayAuthorizationServerService client = new AxwayAuthorizationServerService(
                rpcClient,
                new AxwayCacheStore(tempDir.resolve("axway.json"), new com.fasterxml.jackson.databind.ObjectMapper())
        );

        OperationOutcome outcome = client.dissociateScope("client-1", "scope-1");

        assertTrue(outcome.isSuccess());
        assertEquals(200, outcome.getStatusCode());

        RecordedRequest req1 = take();
        assertRequest(req1, "GET", "/api/portal/v1.2/applications/oauthclient/client-1");

        RecordedRequest req2 = take();
        assertRequest(req2, "GET", "/api/portal/v1.2/applications/app-1/scope");

        RecordedRequest req3 = take();
        assertRequest(req3, "DELETE", "/api/portal/v1.2/applications/app-1/scope/scope-1-id");
    }

    private AuthorizationServerSettings settings(String baseUrl) {
        return AuthorizationServerSettings.from(new com.company.scopehandler.api.config.AppConfig(new java.util.Properties() {{
            setProperty("as.axway.env.dev.baseUrl", baseUrl);
            setProperty("as.axway.auth.username", "user");
            setProperty("as.axway.auth.password", "pass");
        }}), "axway", "dev");
    }

    private RecordedRequest take() {
        try {
            return server.takeRequest();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("request not captured", e);
        }
    }

    private void assertRequest(RecordedRequest request, String method, String path) {
        assertEquals(method, request.getMethod());
        assertEquals(path, request.getPath());
        String expectedAuth = "Basic " + Base64.getEncoder().encodeToString("user:pass".getBytes());
        assertEquals(expectedAuth, request.getHeader("Authorization"));
    }

    private void assertJson(String json, String field, String expected) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);
            JsonNode value = node.get(field);
            if (value == null) {
                throw new IllegalStateException("field missing: " + field);
            }
            assertEquals(expected, value.asText());
        } catch (Exception e) {
            throw new IllegalStateException("invalid json: " + json, e);
        }
    }

    private static MockResponse okJson(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static MockResponse conflictJson(String body) {
        return new MockResponse()
                .setResponseCode(409)
                .addHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static final class AxwayDispatcher extends Dispatcher {
        private final java.util.Map<String, MockResponse> routes = new java.util.HashMap<>();

        AxwayDispatcher whenGet(String path, MockResponse response) {
            routes.put("GET " + path, response);
            return this;
        }

        AxwayDispatcher whenPost(String path, MockResponse response) {
            routes.put("POST " + path, response);
            return this;
        }

        AxwayDispatcher whenDelete(String path, MockResponse response) {
            routes.put("DELETE " + path, response);
            return this;
        }

        @Override
        public MockResponse dispatch(RecordedRequest request) {
            String key = request.getMethod() + " " + request.getPath();
            MockResponse response = routes.get(key);
            if (response != null) {
                return response;
            }
            return new MockResponse().setResponseCode(404);
        }
    }
}
