# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build and run all tests (also enforces Jacoco coverage)
./gradlew build

# Run tests only
./gradlew test

# Run a single test class
./gradlew test --tests "com.kaiqkt.magiapi.integration.UserIntegrationTest"

# Run a single test method
./gradlew test --tests "com.kaiqkt.magiapi.integration.UserIntegrationTest.UserCreation.HappyPath.*"

# Lint check / auto-format
./gradlew ktlintCheck
./gradlew ktlintFormat
```

## Architecture

Spring Boot 3.5.7 / Kotlin / Java 21. Three logical layers with strict dependency rules:

| Layer | Responsibility | May depend on |
|---|---|---|
| `domain` | Pure business rules — entities, interfaces, exceptions | nothing |
| `application` | Orchestrates use cases — controllers, security, config | `domain` only |
| `resources` | Infrastructure — DB, external APIs, file access | `domain` only |

`application` and `resources` never depend on each other. `domain` is completely isolated.

Current package layout within the single module:

```
application/
  web/controllers    → REST endpoints
  web/handlers       → ErrorHandler (global exception → HTTP status mapping)
  security/          → JWT filter + Spring Security config
  config/            → ObjectMapper, WebMVC interceptor
domain/
  services/          → Business logic
  models/            → JPA entities
  repositories/      → Spring Data JPA interfaces (contracts defined in domain)
  exceptions/        → DomainException(ErrorType)
utils/               → TokenUtils, PasswordEncrypt, MetricsUtils
```

## Key Conventions

**Entity IDs** — all use ULID (`UlidCreator.getMonotonicUlid().toString()`), stored as `VARCHAR(26)`. Never use UUID.

**Error handling** — throw `DomainException(ErrorType.SOME_TYPE)`. `ErrorType.message` is auto-derived from the enum name (`EMAIL_ALREADY_EXISTS` → `"email already exists"`). `ErrorHandler` maps each `ErrorType` to an HTTP status.

**Security** — `AuthenticationFilter` validates `Bearer` JWT tokens (HS256, nimbus-jose-jwt). Public routes are declared in `AuthenticationFilter.publicRoutes`. Use `SecurityContext.getUserId()` to retrieve the authenticated user's ID inside services. Password hashing uses Argon2 (`PasswordEncrypt.encoder`).

**Metrics** — inject `MetricsUtils` and call `metricsUtils.counter(name, "status", value)` in services for Prometheus counters.

**Request tracing** — `WebInterceptor` reads `X-Request-Id` header (or generates a UUID) and puts it in MDC for structured logging.

## Testing

**Coverage** — Jacoco enforces 100% line and branch coverage on every `./gradlew test`. Excluded from coverage: `Application`, `application/config`, `web/requests`, `web/responses`, `domain/models`, `domain/dtos`.

**Integration tests** (`src/test/kotlin/.../integration/`) — real Spring context + Testcontainers PostgreSQL (configured via `jdbc:tc:postgresql:15.3:///test_database` in `src/test/resources/application.yml`). Base class is `IntegrationTest`. No Mockito; use MockK only for unit tests. External HTTP dependencies must be mocked with MockServer.

**Unit tests** (`src/test/kotlin/.../unit/`) — no Spring context; use MockK.

**Test organisation** — use `@Nested` inner classes in PascalCase (`UserCreation`, `RequestValidation`, `BusinessRules`, `HappyPath`) to group tests by feature and scenario type. Test method names follow `given … when … then …` in backticks.

**Seeding data** in integration tests — use the repository directly (e.g., `userRepository.save(...)`). Encode passwords with `PasswordEncrypt.encoder.encode(...)`. The base `IntegrationTest.beforeEach` calls `userRepository.deleteAll()` before each test.

## Database

Flyway migrations live in `src/main/resources/db/migration/`. All FK columns and IDs are `VARCHAR(26)` to match ULID length. `user_roles` is a `@ElementCollection` table joined to `users`.
