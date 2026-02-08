# 3. Configuração

[Anterior](architecture.md) | [Índice](README.md) | [Próximo](cli.md)

Configurações via `application.properties` (estilo Spring Boot).

## Propriedades principais
- `as.<name>.env.<env>.baseUrl`: URL do AS no ambiente.
- `as.<name>.auth.username`: usuário de autenticação básica.
- `as.<name>.auth.password`: senha de autenticação básica.
- `as.<name>.timeoutSeconds`: timeout das requisições.

Exemplo:
```properties
as.axway.env.dev.baseUrl=https://axway.dev.example.com
as.axway.auth.username=admin
as.axway.auth.password=secret
as.axway.timeoutSeconds=30
```

## Credenciais
- Podem ser informadas por arquivo (`credentials`) ou leitura em runtime.
- Arquivo no formato `env`:
```
username=admin
password=secret
```

[Anterior](architecture.md) | [Índice](README.md) | [Próximo](cli.md)
