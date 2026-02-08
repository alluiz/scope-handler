# 11. Boas Práticas

[Anterior](tests.md) | [Índice](README.md) | [Próximo](README.md)

## Clean Code
- Métodos curtos e com propósito único.
- Nomes explícitos para parâmetros e variáveis.
- DTOs tipados em vez de `Map`.

## SOLID
- **SRP**: classes focadas em uma responsabilidade (ex.: logger, cache, client).
- **OCP**: adicionar novos AS sem alterar o core.
- **DIP**: integrações via `AuthorizationServerService`.

## Observabilidade
- Logs estruturados.
- Auditoria persistente.
- Resumo de execução ao final.

[Anterior](tests.md) | [Índice](README.md) | [Próximo](README.md)
