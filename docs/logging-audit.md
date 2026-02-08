# 5. Logs e Auditoria

[Anterior](cli.md) | [Índice](README.md) | [Próximo](cache-resilience.md)

## Console
- Logs por operação somente em **debug**.
- Logs parciais mostram progresso e contadores `ok/fail/skip`.
- Tempo total e parcial é exibido em unidades humanas (s/min/h).

## Auditoria
- Arquivos de auditoria gravam cada operação com status final.
- O log inclui: modo, client, scope, status, duração e thread.

## Axway request log
- Arquivo separado com REQUEST/RESPONSE/EXCEPTION.
- Inclui contexto com IDs (clientId, appId, scope, scopeId).

[Anterior](cli.md) | [Índice](README.md) | [Próximo](cache-resilience.md)
