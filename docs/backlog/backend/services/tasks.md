# Backend / Services

---

## Criar entidade e migration de Service

**Status:** `todo`
**Description:** Criar migration `V6__create_services_table.sql` com tabela `services` (`id`, `project_id`, `name`, `type` POSTGRES/REDIS/RABBITMQ, `config` JSONB, `created_at`). Criar entidade JPA `ManagedService` (evitar conflito com `@Service` do Spring), `ManagedServiceRepository`. FK para `projects(id)`. Índice em `(project_id)`.
**User Story:** As a project owner or admin, I want to define managed services (databases, caches) so that the platform can provision and connect them to my applications.

---

## Implementar criação de Service

**Status:** `todo`
**Description:** Criar `POST /v1/services` com body `{ name, type, config }`. O `ManagedServiceService.create(userId, tenantId, request)` deve: (1) validar membership owner/admin, (2) verificar que não existe serviço com mesmo nome no projeto, (3) persistir o serviço. O campo `config` armazena parâmetros específicos do tipo (ex: versão da imagem, porta customizada).
**User Story:** As a project owner or admin, I want to create a managed service so that it can be provisioned on a server and connected to my applications.

---

## Provisionar Service no servidor via evento

**Status:** `todo`
**Description:** Criar endpoint `POST /v1/services/{id}/provision` com body `{ serverId }`. O `ManagedServiceService.provision(userId, serviceId, serverId)` deve: (1) validar membership, (2) criar um `Event` do tipo `provision_service` com `data = { type, config, name }`, (3) enviar via `AgentWebSocketHandler`. O agent sobe o container via `docker compose`. O `EventService` rastreia o status.
**User Story:** As a project owner or admin, I want to provision a service on a specific server so that it becomes available for applications in that environment.

---

## Injetar variáveis de conexão em Applications

**Status:** `todo`
**Description:** Criar modelo `ServiceConnection` (`id`, `application_id`, `service_id`, `env_var_prefix`) e migration correspondente. `POST /v1/connections` com body `{ applicationId, serviceId, envVarPrefix }` cria o vínculo. Ao fazer deploy, o `DeploymentService` deve buscar todas as `ServiceConnection` da aplicação e incluir as env vars correspondentes no `composeSpec` do evento `deploy`. As variáveis são derivadas do tipo do serviço (ex: `DATABASE_URL`, `REDIS_URL`).
**User Story:** As a developer, I want my application to automatically receive the connection details for linked services as environment variables so that I don't have to configure them manually.
