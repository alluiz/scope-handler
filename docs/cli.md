# 4. CLI e Operação

[Anterior](configuration.md) | [Índice](README.md) | [Próximo](logging-audit.md)

A CLI é baseada em **Picocli** e suporta execução em batch com opções de modo, fontes de input e controle de cache.

## Modo
- `--associate` ou `--dissociate` (exclusivos).

## Inputs
- `--clients`: lista via CLI.
- `--clients-file`: arquivo com um client por linha.
- `--scopes`: lista via CLI.
- `--scopes-file`: arquivo com um escopo por linha.
- Pode combinar arquivo + CLI.

## Execução de teste
- `--test` carrega `clients.txt` e `scopes.txt` e usa AS `mock` como padrão.

## Exemplos
```bash
bin/scope-handler --associate --as axway --env dev --clients-file clients.txt --scopes-file scopes.txt

bin/scope-handler --dissociate --as axway --env prod --clients client1,client2 --scopes read,write

bin/scope-handler --test --associate
```

[Anterior](configuration.md) | [Índice](README.md) | [Próximo](logging-audit.md)
