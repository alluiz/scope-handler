# 8. Axway Client

[Anterior](concurrency-performance.md) | [Índice](README.md) | [Próximo](api-reference.md)

Implementação concreta em `com.company.scopehandler.providers.axway`.

## Endpoints
- `GET /api/portal/v1.2/applications/oauthclient/{clientId}`
- `GET /api/portal/v1.2/applications/{id}/scope`
- `POST /api/portal/v1.2/applications/{id}/scope`
- `DELETE /api/portal/v1.2/applications/{id}/scope/{scopeId}`

## Fluxo de associate
1. Buscar applicationId pelo clientId.
2. Criar escopo via POST.
3. Retornar `SKIP` se status 409.

## Fluxo de dissociate
1. Buscar applicationId pelo clientId.
2. Consultar scopes da aplicação.
3. Remover scope por id.
4. Retornar `SKIP` se scope não existir.

## Observações
- Sem cache de scope.
- Timeout configurável.
- Logs detalhados em arquivo dedicado.

[Anterior](concurrency-performance.md) | [Índice](README.md) | [Próximo](api-reference.md)
