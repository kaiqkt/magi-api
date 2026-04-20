# Refactoring

> Feature: backend

## Extract Project/Membership Resolution Extension

**Status:** `done`
**Description:** O padrão abaixo se repete em vários serviços e deve ser centralizado para evitar duplicação:

```kotlin
val project = projectService.findByTenantId(tenantId)
val membership = projectService.findMembership(project.id, userId)

if (!membership.hasPermission()) {
    metricsUtils.counter(APPLICATION, STATUS, "insufficient_permissions")
    throw DomainException(ErrorType.INSUFFICIENT_PERMISSION)
}
```

Criar um método em `ProjectService` (ex: `resolveAuthorizedContext(tenantId, userId): Pair<Project, Membership>`) que encapsule as duas chamadas e já lance `DomainException(ErrorType.INSUFFICIENT_PERMISSION)` se o membership não tiver permissão. O registro de métrica antes do throw pode ficar no serviço chamador, já que o nome do counter varia por domínio (APPLICATION, PROJECT, etc.). Atualizar todos os pontos de uso após a extração.
**User Story:** As a developer, I want a single call to resolve project and membership context so that I don't repeat the same two-line boilerplate across every service method.
