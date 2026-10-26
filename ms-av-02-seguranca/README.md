# MS-AV-02 – Segurança e Observabilidade

Serviço de login em Spring Boot (RESTful) com controle de bloqueio, roles via `JdbcUserDetailsManager`, métricas no Prometheus e dashboard no Grafana.

## Stack

- Java 17 + Spring Boot 3.5 (Web, Security, JDBC, Actuator)
- H2 em memória (mesmo padrão das aulas)
- Micrometer + Prometheus + Grafana
- springdoc-openapi (Swagger)
- Docker Compose para subir tudo junto

## Como rodar

Pré-requisito: Docker Desktop aberto.

```bash
docker compose up --build
```

| Serviço | URL |
|---|---|
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| OpenAPI (JSON) | http://localhost:8080/v3/api-docs |
| Métricas | http://localhost:8080/actuator/prometheus |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin / admin) |

O dashboard **Login Service - Segurança e Observabilidade** já sobe provisionado no Grafana.

Para gerar dados no dashboard:

```bash
# Linux/Mac/Git Bash
bash scripts/gerar-trafego.sh

# Windows PowerShell
powershell -ExecutionPolicy Bypass -File scripts\gerar-trafego.ps1
```

Também tem o `requests.http` para testar pelo VS Code (extensão REST Client).

## Usuários de teste

**Tabela `usuarios`** (usados no `/login` e `/trocarsenha`):

| username | senha |
|---|---|
| joao | 123456 |
| bianca | 123456 |
| maria | abc123 |
| pedro | abc123 |

**Tabelas `users`/`authorities`** (`JdbcUserDetailsManager`, autenticação Basic nos endpoints protegidos):

| username | senha | role |
|---|---|---|
| admin | admin123 | ROLE_ADMIN |
| manutencao | manut123 | ROLE_MANUTENCAO |

## Endpoints

| Método | Rota | Acesso | Retornos |
|---|---|---|---|
| POST | `/login` | público | 200 válido · 401 inválido ou bloqueado |
| PUT | `/trocarsenha/{userid}` | público | 200 · 400 nova inválida · 401 atual incorreta · 404 · 423 bloqueado |
| POST | `/` | ROLE_MANUTENCAO | 201 · 400 · 409 já existe · 401/403 |
| GET | `/` | ROLE_MANUTENCAO | 200 lista de usuários · 401/403 |
| PUT | `/unlock/{userid}` | ROLE_ADMIN | 200 · 404 · 401/403 |

Corpos JSON:

```json
POST /login               { "username": "joao", "senha": "123456" }
PUT  /trocarsenha/joao    { "atual": "123456", "nova": "nova123" }
POST /                    { "username": "carlos", "senha": "senha123" }
```

## Regra de bloqueio

- Cada senha errada incrementa `login_attempts.failed_attempts`.
- Na **segunda** falha (falhou mais de uma vez) o usuário fica com `locked = TRUE`.
- Usuário bloqueado recebe 401 no login, mesmo com a senha certa, até um admin chamar `PUT /unlock/{userid}`.
- Login com sucesso zera as falhas.

## Indicadores

| Métrica (Prometheus) | Tipo | O que mede |
|---|---|---|
| `login_validos_total` | counter | logins válidos |
| `login_invalidos_total` | counter | logins inválidos (senha errada, usuário inexistente ou bloqueado) |
| `usuarios_bloqueados` | gauge | `COUNT(*)` de `login_attempts` com `locked = TRUE` |

## Dashboard Grafana

- Dashboard provisionado: `observabilidade/grafana/dashboards/login-dashboard.json`
- Dashboard exportado pelo Grafana (export as code): `observabilidade/grafana/dashboard-exportado.json`
- Print: `docs/dashboard.png`

![Dashboard](docs/dashboard.png)

## Decisões de implementação

- **Vírgula no SQL:** o script do enunciado tem uma vírgula sobrando depois de `updated_at` em `login_attempts`, que foi removida.
- **Duas bases de usuários:** o enunciado pede que `/login` valide pela tabela `usuarios` e que os endpoints com role usem `JdbcUserDetailsManager`. Por isso, os usuários "do sistema" ficam em `usuarios`, e quem administra (admin/manutencao) fica no schema padrão `users`/`authorities`, com senha em BCrypt.
- **Senha em `usuarios`:** a coluna é `VARCHAR(10)`, que não comporta um hash BCrypt (60 caracteres). Por isso, a senha nessa tabela fica em texto puro, como o enunciado define.
- **Métricas:** os counters ficam em memória e zeram ao reiniciar a aplicação, assim como o H2.

## Estrutura

```
src/main/java/br/espm/ms/seguranca
├── config/       SecurityConfig, OpenApiConfig, CargaUsuariosRoles
├── controller/   UsuarioController
├── dto/          records de request/response
├── metrics/      LoginMetrics (counters + gauge)
└── service/      UsuarioService (regras de login/bloqueio)
src/main/resources  application.properties, schema.sql, data.sql
observabilidade/    prometheus.yml + provisioning do Grafana
scripts/            gerar-trafego (.sh e .ps1)
```
