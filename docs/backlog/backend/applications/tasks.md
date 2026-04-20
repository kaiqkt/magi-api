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
**Description:** O fluxo de provisionamento deve ser executado em 3 passos sequenciais com rollback compensatório em caso de falha em qualquer etapa. Se o projeto não tiver `GitAccount` associado, lançar `DomainException(ErrorType.GIT_ACCOUNT_NOT_FOUND)` antes de iniciar qualquer passo.

**Passo 1 — Criar repositório**
Chamar `GithubClient` para criar um repositório privado no GitHub com o nome derivado do campo `name` da aplicação, usando o `access_token` do `GitAccount` do projeto (`POST /user/repos`).
- Rollback: nenhum (é o primeiro passo).

**Passo 2 — Inicializar repositório**
Usar a GitHub Contents API (`PUT /repos/{owner}/{repo}/contents/.github/workflows/magi-build.yml`) para criar o arquivo de workflow diretamente via API, sem clone local, enviando o conteúdo em Base64. O workflow deve ser acionado via `workflow_dispatch` com input `image_tag`, fazer build da imagem Docker, publicá-la no GHCR e notificar o Magi API via `POST /v1/webhooks/build` com `application_version_id`, `status` e `image_tag`.
- Rollback se falhar: deletar o repositório criado no Passo 1 (`DELETE /repos/{owner}/{repo}`).

**Passo 3 — Persistir no banco de dados**
Salvar a entidade `Application` com o `repositoryUrl` retornado pelo Passo 1.
- Rollback se falhar: deletar o arquivo de workflow (ou o repositório inteiro) criado nos Passos 1 e 2.

**Implementação sugerida:** extrair os 3 passos em métodos privados dentro de `ApplicationService` e encadear com blocos `try/catch` que invocam os compensadores na ordem inversa. Não usar `@Transactional` para cobrir chamadas externas — a transação do banco deve ser aberta apenas no Passo 3.
**User Story:** As a project owner or admin, I want the platform to automatically create the GitHub repository and configure the build pipeline when I register an application so that I can trigger builds immediately without manual setup.
