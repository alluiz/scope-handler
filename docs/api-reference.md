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

### 2) Criar AuthorizationServerClient
```java
AuthorizationServerFactory factory = new AuthorizationServerFactory();
Path cacheDir = Path.of("/var/lib/scope-handler/cache");
AuthorizationServerClient client = factory.create(asName, env, config, cacheDir);
```

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
  AuthorizationServerClient client = factory.create("axway", "prod", config, cacheDir);
  OperationOutcome outcome = client.associateScope(msg.clientId(), msg.scope());
  // tratar OK / FAIL / SKIP
}
```

## Exemplo em Lambda (serverless)
```java
public class Handler {
  private final AuthorizationServerClient client;

  public Handler() {
    AppConfig config = AppConfig.builder()
        .set("as.axway.env.prod.baseUrl", "https://axway.prod.example.com")
        .set("as.axway.auth.username", System.getenv("AS_USERNAME"))
        .set("as.axway.auth.password", System.getenv("AS_PASSWORD"))
        .build();
    client = new AuthorizationServerFactory().create("axway", "prod", config, Path.of("/tmp/cache"));
  }

  public String handle(Request input) {
    OperationOutcome outcome = client.dissociateScope(input.clientId(), input.scope());
    return outcome.getStatus().name();
  }
}
```

## Boas práticas de integração
- Reutilize o `AuthorizationServerClient` entre chamadas quando possível.
- Use cache em disco quando a execução puder ser interrompida.
- Configure credenciais por arquivo ou segredo gerenciado.
- Em serverless, use `/tmp` para cache/logs.

[Anterior](axway-client.md) | [Índice](README.md) | [Próximo](tests.md)
