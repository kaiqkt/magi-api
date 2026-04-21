# magi-api

Platform API for managing AI agents — handles user auth, project and team management, GitHub account association, and server (agent runtime) provisioning with WebSocket-based event delivery.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin / Java 21 |
| Framework | Spring Boot 3.5.7 |
| Database | PostgreSQL (Flyway migrations) |
| HTTP Client | Fuel 2.3.1 |
| Auth | nimbus-jose-jwt 10.5 (HS256 JWT) |
| WebSocket | Spring WebSocket |
| Metrics | Micrometer + Prometheus |
| Infra | Docker Compose |

## Getting Started

### Prerequisites

- Java 21
- Docker

### Running locally

```bash
# Start PostgreSQL
docker compose up -d

# Run the application (profile: local)
./gradlew bootRun
```

### Running tests

```bash
# Run all tests (also enforces Jacoco coverage)
./gradlew test

# Run a single test class
./gradlew test --tests "com.kaiqkt.magiapi.integration.UserIntegrationTest"

# Run a single test method
./gradlew test --tests "com.kaiqkt.magiapi.integration.UserIntegrationTest.UserCreation.HappyPath.*"
```

### Lint

```bash
./gradlew ktlintCheck   # check
./gradlew ktlintFormat  # auto-fix
```

## Architecture

Three logical layers with strict dependency rules:

```
application/   → orchestrates use cases (controllers, security, config)
domain/        → pure business rules (entities, services, interfaces)
resources/     → infrastructure (DB, external APIs)
```

`application` and `resources` depend on `domain` only and never on each other.

## Key Concepts

**Multi-tenancy** — each project is a tenant identified by a slug extracted from the `Host` header subdomain (`my-project.localhost.com` → `my-project`).

**Agent communication** — agents running on user servers connect via WebSocket (`ws://.../v1/ws/agent`) using a per-server `agentToken`. Commands are delivered in real time; events queued while the agent is offline are drained on reconnect.

**Build flow** — builds are triggered via `POST /v1/applications/{id}/builds`, which dispatches a `workflow_dispatch` event to GitHub Actions. The returned `workflow_run_id` is stored on the `ApplicationVersion` and exposed to the frontend for status tracking via the GitHub Actions badge.

**Deployments** — a deployment takes an existing `ApplicationVersion` and sends a `deploy` event to the target server's agent via WebSocket.

## API Overview

| Method | Endpoint | Description |
|---|---|---|
| POST | `/v1/users` | Create account |
| POST | `/v1/auth` | Login → JWT |
| POST | `/v1/projects` | Create project |
| POST | `/v1/projects/members` | Invite member |
| POST | `/v1/projects/git-accounts` | Associate GitHub account |
| POST | `/v1/servers?env=DEV` | Register server |
| POST | `/v1/applications` | Create application |
| POST | `/v1/applications/{id}/builds` | Trigger build |
| POST | `/v1/deployments` | Deploy a version |
| GET | `/v1/servers` | List project servers |
| GET | `/v1/deployments` | Deployment history |

## Configuration

Default configuration lives in `src/main/resources/application-local.yml`. Key properties:

```yaml
authentication:
  access-token-ttl: 6000000
  access-token-secret: <secret>

github:
  url: https://api.github.com
  content-path: .github/workflows/magi-ci.yml
```
