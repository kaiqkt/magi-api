# Backend / Applications

---

## Criar entidade e migration de Application

**Status:** `done`
**Description:** Criar migration `V3__create_applications_table.sql` com tabela `applications` (`id`, `project_id`, `name`, `repo_url`, `branch`, `created_at`). Criar entidade JPA `Application`, `ApplicationRepository`. FK para `projects(id)`. Índice em `(project_id)`.
**User Story:** As a developer, I want applications to be persisted so that they can be versioned and deployed to servers.

---

## Implementar criação de Application

**Status:** `done`
**Description:** Criar `ApplicationRequest` (name, repo_url, branch), `ApplicationService.create(userId, tenantId, request)` com validação de membership (owner/admin), `ApplicationController` com `POST /v1/applications` resolvendo tenant via `TenantContext`. Lançar `DomainException(ErrorType.APPLICATION_ALREADY_EXISTS)` se já existir aplicação com mesmo nome no projeto. Adicionar ao `ErrorHandler` e ao `ErrorType`.
**User Story:** As a project owner or admin, I want to register an application so that I can manage its deployments through the platform.

---

## Implementar listagem de Applications

**Status:** `todo`
**Description:** Adicionar `GET /v1/applications` no `ApplicationController` retornando as aplicações do projeto resolvido pelo tenant. Qualquer membro (incluindo MEMBER) pode listar.
**User Story:** As a project member, I want to see all applications in my project so that I can select one for deployment.

---

## Provisionar repositório e workflow de build no GitHub ao criar Application

**Status:** `done`
**Description:** Ao criar uma Application, o `ApplicationService.create` deve, após persistir a entidade: (1) buscar o `GitAccount` associado ao projeto para obter o `access_token` e `username`; (2) chamar o `GithubClient` para criar o repositório no GitHub com o nome derivado do campo `name` da aplicação (ex: `magi-<slug>`), usando o token do `GitAccount`; (3) fazer push de um arquivo `.github/workflows/magi-build.yml` no repositório recém-criado — o workflow deve ser acionado via `workflow_dispatch` com input `image_tag`, construir a imagem Docker, publicá-la no GHCR e notificar o Magi API via `POST /v1/webhooks/build` com `application_version_id`, `status` e `image_tag`; (4) persistir o `repo_url` gerado na entidade `Application`. Se o projeto não tiver `GitAccount` associado, lançar `DomainException(ErrorType.GIT_ACCOUNT_NOT_FOUND)` antes de criar a Application. Adicionar `GIT_ACCOUNT_NOT_FOUND` ao `ErrorType` e ao `ErrorHandler` (404).
**User Story:** As a project owner or admin, I want the platform to automatically create the GitHub repository and configure the build pipeline when I register an application so that I can trigger builds immediately without manual setup.
