# Scope Handler

Batch para associacao e desassociacao de escopos OAuth 2.0 em Authorization Server.

## Requisitos
- Java 17+
- Maven 3.9+

## Compilacao
```bash
mvn -q -DskipTests package
```

## Executar (binario com bin/ + lib/)
Depois do `package`, o appassembler gera:
- `target/app/bin/scope-handler` (Linux/macOS)
- `target/app/bin/scope-handler.bat` (Windows)

```bash
./target/app/bin/scope-handler --help
```

### Execucao rapida de teste
Coloque `clients.txt` e `scopes.txt` no diretório atual e execute:
```bash
./target/app/bin/scope-handler --test --mode associate --user admin --password secret
```

Para ver logs detalhados por operacao, adicione `--debug`.
Para ignorar o cache de execucoes anteriores, use `--ignore-cache`.

Para adicionar ao PATH (Linux/macOS):
```bash
export PATH=\"$PWD/target/app/bin:$PATH\"
```

### Exemplo associar
```bash
./target/app/bin/scope-handler \
  --as mock \
  --env dev \
  --mode associate \
  --clients clientA,clientB \
  --scopes read,write \
  --user admin \
  --password secret \
  --create-scope
```

### Exemplo desassociar (critico)
```bash
./target/app/bin/scope-handler \
  --as mock \
  --env prod \
  --mode dissociate \
  --clients-file ./clients.txt \
  --scopes-file ./scopes.txt \
  --user admin \
  --password secret \
  --confirm
```

## Configuracao
Arquivo padrao: `src/main/resources/application.properties`. Pode ser sobrescrito com `--config`.

Principais chaves:
- `as.<nome>.env.<ambiente>.baseUrl`
- `as.<nome>.auth.username` / `as.<nome>.auth.password`

Para Axway (API Manager), use `--as axway` e configure apenas `baseUrl` e credenciais.
Timeout do WebClient:
- `as.axway.timeoutSeconds` (default 30)
- `as.axway.logFile` (arquivo de log detalhado das requisicoes)
- `batch.threads.max`
- `batch.threads.threshold`
- `audit.dir`

## Credenciais
Por padrão, se `--user` e `--password` não forem informados, o aplicativo tenta ler o arquivo `./credentials`.

Exemplo de `credentials` (formato env):
```env
AS_USERNAME=admin
AS_PASSWORD=secret
```

Também é possível informar um arquivo diferente via `--credentials-file`.
Se ainda não houver credenciais, o programa solicita em tempo de execução.

## Auditoria e Relatorio
- Auditoria CSV: `audit-YYYYMMDD-HHMMSS.csv`
- Relatorio consolidado: `report-YYYYMMDD-HHMMSS.txt`

## Cache de execucao (resume)
O sistema grava um cache em disco para pular operacoes ja executadas em execucoes anteriores.
Arquivo gerado: `audit/cache/resume-cache-<as>-<env>.txt`.

Para ignorar o cache:
```bash
./target/app/bin/scope-handler --ignore-cache ...
```

## Cache Axway
Para Axway, o client grava cache persistente de `clientId -> applicationId` e `applicationId -> scopeId` em:
`audit/cache/axway-cache-<as>-<env>.json`.

## Observacoes
- `--create-scope` so se aplica ao modo associar.
- A desassociacao exige confirmacao interativa ou `--confirm`.
- Multi-thread e habilitado quando `total >= batch.threads.threshold`.
- A implementacao atual do AS e mockada e simula latencia de 100ms.
