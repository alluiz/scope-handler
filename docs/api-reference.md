# 9. API Reference

[Anterior](axway-client.md) | [Índice](README.md) | [Próximo](tests.md)

## Objetivo
Este guia descreve como usar a solução como **biblioteca Java**, por exemplo em um worker que consome mensagens ou em execução serverless (Lambda).

## Dependência
Se publicar internamente, consuma o artefato Maven do projeto. Exemplo de dependência:
```xml
<dependency>
  <groupId>com.company</groupId>
  <artifactId>scope-handler</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Pacotes principais
- `com.company.scopehandler.api.usecases`: orquestração do batch.
- `com.company.scopehandler.api.services`: execução concorrente e relatórios.
- `com.company.scopehandler.api.ports`: contratos para integração com AS.
- `com.company.scopehandler.api.config`: configuração por propriedades e builder.

## Inicialização programática
### 1) Carregar configuração (Builder)
```java
AppConfig config = AppConfig.builder()
    .set("as.axway.env.prod.baseUrl", "https://axway.prod.example.com")
    .set("as.axway.auth.username", "admin")
    .set("as.axway.auth.password", "secret")
    .set("as.axway.timeoutSeconds", "30")
    .build();
String asName = "axway";
String env = "prod";
AuthorizationServerSettings settings = AuthorizationServerSettings.from(config, asName, env);
```

### 2) Criar AuthorizationServerService
```java
AuthorizationServerFactory factory = AuthorizationServerFactory.builder()
    .register("custom", () -> new CustomAuthorizationServerService(settings))
    .build();
AuthorizationServerService client = factory.create(asName);
```

## Registro de AS customizado (Registration Pattern)
```java
AuthorizationServerFactory factory = AuthorizationServerFactory.builder()
    .register("custom", () -> new CustomAuthorizationServerService(settings))
    .build();
```

Observações:
- O registry inicia vazio. Registre os AS desejados no builder.
- AS como `axway` devem ser registrados pelo cliente da API.

### 3) Executar operações pontuais
```java
OperationOutcome result = client.associateScope("client-1", "scope-1");
if (result.isSuccess()) {
  // ok
}
```

## Usando o Use Case (batch)
```java
List<String> clients = List.of("client-1", "client-2");
List<String> scopes = List.of("read", "write");

BatchPlannerService planner = new BatchPlannerService();
BatchExecutorService executor = new BatchExecutorService();
ExecuteBatchUseCase useCase = new ExecuteBatchUseCase(planner, executor);

ExecutionCache cache = ExecutionCache.load(cacheFile, true);
BatchReport report = useCase.execute(
    clients,
    scopes,
    strategy,
    auditService,
    threshold,
    threads,
    false,
    cache
);
```

## Exemplo em worker de fila
```java
public void handleMessage(Message msg) {
  AuthorizationServerService client = factory.create("axway");
  OperationOutcome outcome = client.associateScope(msg.clientId(), msg.scope());
  // tratar OK / FAIL / SKIP
}
```

## Exemplo em Lambda (serverless)
```java
public class Handler {
  private final AuthorizationServerService client;

  public Handler() {
    AppConfig config = AppConfig.builder()
        .set("as.axway.env.prod.baseUrl", "https://axway.prod.example.com")
        .set("as.axway.auth.username", System.getenv("AS_USERNAME"))
        .set("as.axway.auth.password", System.getenv("AS_PASSWORD"))
        .build();
    AuthorizationServerSettings axwaySettings = AuthorizationServerSettings.from(config, "axway", "prod");
    HttpRequestLogger logger = new HttpRequestLogger(Path.of("/tmp/cache/axway.log"));
    AuthorizationServerFactory factory = AuthorizationServerFactory.builder()
        .register("axway", () -> {
            WebClient baseClient = HttpWebClientFactory.build(logger);
            WebClient webClient = HttpWebClientFactory.mutate(baseClient, builder -> builder
                .baseUrl(axwaySettings.getBaseUrl())
                .defaultHeader("Authorization", "Basic ...")
                .defaultHeader("Accept", "application/json"));
            return new AxwayAuthorizationServerService(
                new AxwayAuthorizationServerClient(webClient, Duration.ofSeconds(30)),
                new AxwayCacheStore(Path.of("/tmp/cache/axway.json"), new ObjectMapper())
            );
        })
        .build();
    client = factory.create("axway");
  }

  public String handle(Request input) {
    OperationOutcome outcome = client.dissociateScope(input.clientId(), input.scope());
    return outcome.getStatus().name();
  }
}
```

## Boas práticas de integração
- Reutilize o `AuthorizationServerService` entre chamadas quando possível.
- Use cache em disco quando a execução puder ser interrompida.
- Configure credenciais por arquivo ou segredo gerenciado.
- Em serverless, use `/tmp` para cache/logs.

[Anterior](axway-client.md) | [Índice](README.md) | [Próximo](tests.md)
