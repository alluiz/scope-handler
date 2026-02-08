# 6. Cache e Resiliência

[Anterior](logging-audit.md) | [Índice](README.md) | [Próximo](concurrency-performance.md)

## Cache de retomada (resume)
- Persistido em `cache/resume-cache-<as>-<env>.txt`.
- Só registra operações com sucesso (`OK`).
- Usado para pular operações já concluídas.
- Se a execução termina sem interrupções, o cache é removido.
- Pode ser ignorado via flag de CLI.

## Axway cache de applicationId
- Mapeia `clientId -> applicationId` em arquivo local.
- Evita consulta repetida de application em execução longa.
- Não há cache de scope por risco de inconsistência.

[Anterior](logging-audit.md) | [Índice](README.md) | [Próximo](concurrency-performance.md)
