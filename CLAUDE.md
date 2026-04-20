# magi-api

## What Is This

Platform API for managing AI agents — handles user auth, project/team management, GitHub account association, and server (agent runtime) provisioning with WebSocket-based event delivery.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin / Java 21 |
| Framework | Spring Boot 3.5.7 |
| Database | PostgreSQL (Flyway migrations) |
| HTTP Client | Fuel 2.3.1 |
| Auth | nimbus-jose-jwt 10.5 (HS256 JWT) |
| WebSocket | Spring WebSocket (built-in) |
| Metrics | Micrometer + Prometheus |
| Infra | Docker Compose (`docker-compose.yml` — runs PostgreSQL on port 5432) |

## How to Run

```bash
# Run tests (also enforces Jacoco coverage)
./gradlew test

# Run a single test class
./gradlew test --tests "com.kaiqkt.magiapi.integration.UserIntegrationTest"

# Run a single test method
./gradlew test --tests "com.kaiqkt.magiapi.integration.UserIntegrationTest.UserCreation.HappyPath.*"

# Lint check / auto-format
./gradlew ktlintCheck
./gradlew ktlintFormat

# Build (includes tests + coverage)
./gradlew build
```

## Project Structure

```
src/main/kotlin/com/kaiqkt/magiapi/
  Application.kt
  application/
    config/           → ObjectMapper, WebMVC config, WebSocketConfig
    exceptions/       → InvalidRequestException
    security/         → AuthenticationFilter, SecurityConfig, SecurityContext, MagiAuthentication
    web/
      controllers/    → REST endpoints (User, Authentication, Project, Server)
      handlers/       → ErrorHandler (global exception → HTTP status), AgentWebSocketHandler
      interceptors/   → WebInterceptor (request tracing), TenantFilter, TenantContext, AgentHandshakeInterceptor
      requests/       → request DTOs
      responses/      → response DTOs
  domain/
    config/           → AuthenticationProperties (@ConfigurationProperties — exception to domain purity)
    dtos/             → AuthenticationDto, TokenDto, GitUserDto
    exceptions/       → DomainException, ErrorType
    gateways/         → GitGateway (interface)
    models/           → JPA entities + enums
    repositories/     → Spring Data JPA interfaces
    services/         → UserService, AuthenticationService, ProjectService, GitService, ServerService
  resources/
    github/
      clients/        → GithubClient (Fuel HTTP)
      impl/           → GithubGatewayImpl
      responses/      → GithubUserResponse
    exceptions/       → UnexpectedResourceException
  ext/                → String extensions (slugify)
  utils/              → TokenUtils (@Component, JWT), MetricsUtils (@Component, Micrometer), PasswordEncrypt
```

### Architecture

Three logical layers with strict dependency rules:

| Layer | Responsibility | May depend on |
|---|---|---|
| `domain` | Pure business rules — entities, interfaces, exceptions | nothing |
| `application` | Orchestrates use cases — controllers, security, config | `domain` only |
| `resources` | Infrastructure — DB, external APIs, file access | `domain` only |

`application` and `resources` never depend on each other. `domain` is completely isolated.

## Key Conventions

**Entity IDs** — all use ULID (`UlidCreator.getMonotonicUlid().toString()`), stored as `VARCHAR(26)`. Never use UUID.

**Error handling** — throw `DomainException(ErrorType.SOME_TYPE)`. `ErrorType.message` is auto-derived from the enum name (`EMAIL_ALREADY_EXISTS` → `"email already exists"`). `ErrorHandler` maps each `ErrorType` to an HTTP status.

**Security** — `AuthenticationFilter` validates `Bearer` JWT tokens (HS256, nimbus-jose-jwt). Public routes are declared in `AuthenticationFilter.publicRoutes`. Use `SecurityContext.getUserId()` to retrieve the authenticated user's ID inside services. Password hashing uses Argon2 (`PasswordEncrypt.encoder`).

**Tenant resolution** — `TenantFilter` extracts the project slug from the `Host` header subdomain (e.g., `my-project.localhost.com` → `my-project`). Use `TenantContext.getTenant()` inside controllers.

**Metrics** — inject `MetricsUtils` and call `metricsUtils.counter(name, "status", value)` in services for Prometheus counters.

**Request tracing** — `WebInterceptor` reads `X-Request-Id` header (or generates a UUID) and puts it in MDC for structured logging.

**WebSocket auth** — agents connect via `ws://.../v1/ws/agent`. `AgentHandshakeInterceptor` validates the `Authorization: Bearer <agentToken>` header against the `Server` entity's `agentToken` field and stores the resolved `serverId` in the WebSocket session attributes.

## Testing

**Coverage** — Jacoco enforces 100% line and branch coverage on every `./gradlew test`. Excluded from coverage: `Application`, `application/config`, `web/requests`, `web/responses`, `domain/models`, `domain/dtos`.

**Integration tests** (`src/test/kotlin/.../integration/`) — real Spring context + Testcontainers PostgreSQL (`jdbc:tc:postgresql:15.3:///test_database` in `src/test/resources/application.yml`). Base class is `IntegrationTest`. No Mockito; use MockK only for unit tests. External HTTP dependencies must be mocked with MockServer (port 8081).

**Unit tests** (`src/test/kotlin/.../unit/`) — no Spring context; use MockK.

**Test organisation** — use `@Nested` inner classes in PascalCase (`UserCreation`, `RequestValidation`, `BusinessRules`, `HappyPath`) to group tests by feature and scenario type. Test method names follow `given … when … then …` in backticks.

**Seeding data** in integration tests — use repositories directly (e.g., `userRepository.save(...)`). Encode passwords with `PasswordEncrypt.encoder.encode(...)`. `IntegrationTest.beforeEach` deletes in FK-safe order: memberships → gitAccounts → servers → projects → users.

**MockServer helpers** — external HTTP APIs are mocked via `object` classes extending `MockServerHolder` (e.g., `GithubHelper`). Each helper scopes its `reset()` to its own domain path using a regex pattern.

## Database

Flyway migrations live in `src/main/resources/db/migration/`. All FK columns and IDs are `VARCHAR(26)` to match ULID length. `user_roles` is a `@ElementCollection` table joined to `users`.

## Current Focus

> Managed via the `/backlog` skill. See [docs/backlog/index.md](docs/backlog/index.md) for the full task list.
> System design documents live in [docs/system-design/](docs/system-design/). Use `/system-design [topic]` to create or `/system-design refine [topic]` to update a design, and `/system-design tasks [topic]` to bulk-import backlog tasks from it.

## Commit Conventions

Follows [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/).

Format: `<type>[optional scope]: <description>`

| Type | Use when |
|------|----------|
| `feat` | New feature (MINOR in SemVer) |
| `fix` | Bug fix (PATCH in SemVer) |
| `docs` | Documentation only |
| `style` | Formatting, whitespace — no logic change |
| `refactor` | Code restructure without feature or fix |
| `perf` | Performance improvement |
| `test` | Adding or fixing tests |
| `chore` | Tooling, dependencies, config |
| `build` | Build system or dependency changes |
| `ci` | CI/CD configuration changes |
| `revert` | Reverts a previous commit |

Breaking changes: append `!` before the colon (`feat!:`) and add a `BREAKING CHANGE:` footer.

## Preferences

- No emojis in code or comments
- Keep responses concise and practical
- Show me the "why" when suggesting architectural decisions
- When I ask about system design, explain trade-offs not just the "right" answer
- If I'm building something that has a well-known anti-pattern, warn me
