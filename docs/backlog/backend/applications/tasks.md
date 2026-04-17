# Backend / Applications

---

## Criar entidade e migration de Application

**Status:** `todo`
**Description:** Criar migration `V3__create_applications_table.sql` com tabela `applications` (`id`, `project_id`, `name`, `repo_url`, `branch`, `created_at`). Criar entidade JPA `Application`, `ApplicationRepository`. FK para `projects(id)`. Índice em `(project_id)`.
**User Story:** As a developer, I want applications to be persisted so that they can be versioned and deployed to servers.

---

## Implementar criação de Application

**Status:** `todo`
**Description:** Criar `ApplicationRequest` (name, repo_url, branch), `ApplicationService.create(userId, tenantId, request)` com validação de membership (owner/admin), `ApplicationController` com `POST /v1/applications` resolvendo tenant via `TenantContext`. Lançar `DomainException(ErrorType.APPLICATION_ALREADY_EXISTS)` se já existir aplicação com mesmo nome no projeto. Adicionar ao `ErrorHandler` e ao `ErrorType`.
**User Story:** As a project owner or admin, I want to register an application so that I can manage its deployments through the platform.

---

## Implementar listagem de Applications

**Status:** `todo`
**Description:** Adicionar `GET /v1/applications` no `ApplicationController` retornando as aplicações do projeto resolvido pelo tenant. Qualquer membro (incluindo MEMBER) pode listar.
**User Story:** As a project member, I want to see all applications in my project so that I can select one for deployment.
