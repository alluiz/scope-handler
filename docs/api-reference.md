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
- `com.company.scopehandler.usecases`: orquestração do batch.
- `com.company.scopehandler.services`: execução concorrente e relatórios.
- `com.company.scopehandler.ports`: contratos para integração com AS.
- `com.company.scopehandler.config`: configuração por propriedades.

## Inicialização programática
### 1) Carregar configuração
```java
AppConfig config = AppConfig.load(Path.of("/etc/scope-handler/application.properties"));
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
InputResolverService resolver = new InputResolverService();
AuditService auditService = new AuditService(...);
BatchExecutorService executor = new BatchExecutorService(...);
ExecuteBatchUseCase useCase = new ExecuteBatchUseCase(resolver, executor, auditService);

ExecutionCache cache = ExecutionCache.load(cacheFile, true);
BatchSummary summary = useCase.execute(config, asName, env, mode, inputs, cache);
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
    AppConfig config = AppConfig.load(Path.of("/var/task/application.properties"));
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
