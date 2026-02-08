# 10. Testes

[Anterior](api-reference.md) | [Índice](README.md) | [Próximo](best-practices.md)

## Estratégia
- Testes unitários com **JUnit 5**.
- MockWebServer para simular Axway com dispatcher tipo *when*.
- Validação de parâmetros de entrada e payloads JSON.

## Tipos cobertos
- Serviços de batch, auditoria e input.
- Axway client: fluxos de associate/dissociate, incluindo SKIP e falhas.

## Execução
```bash
mvn test
```

[Anterior](api-reference.md) | [Índice](README.md) | [Próximo](best-practices.md)
