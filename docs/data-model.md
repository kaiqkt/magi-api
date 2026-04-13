# Magi — Modelagem de Dados

## Diagrama de Relacionamentos

```
User ──────────────────────────── ProjectMembership
                                        │
                                        ▼
                                     Project ──── GitHubAccount
                                        │
                                       Server (environment obrigatório)
```

---

## Entidades

---

### User

| Campo | Tipo | Restrições |
|---|---|---|
| id | UUID | PK, not null |
| email | VARCHAR(255) | unique, not null, formato e-mail válido |
| password_hash | VARCHAR(255) | not null |
| name | VARCHAR(100) | not null, min 2 chars |
| nickname | VARCHAR(30) | unique, not null, min 3 chars, apenas letras, números e `_` |
| created_at | TIMESTAMP | not null |

**Validações:**
- `email`: formato RFC 5322, lowercase antes de persistir
- `nickname`: regex `^[a-z0-9_]{3,30}$`
- `password` (antes do hash): min 8 chars, ao menos 1 letra e 1 número

---

### Project

| Campo      | Tipo | Restrições |
|------------|---|---|
| id         | UUID | PK, not null |
| name       | VARCHAR(100) | not null, min 3 chars |
| tenant     | VARCHAR(50) | unique, not null, min 3 chars |
| created_by | UUID | FK → User, not null |
| created_at | TIMESTAMP | not null |

**Validações:**
- `tenant`: gerado automaticamente a partir do `name`, regex `^[a-z0-9-]{3,50}$`, único globalmente
- `name`: sem leading/trailing whitespace

---

### ProjectMembership

| Campo | Tipo | Restrições |
|---|---|---|
| id | UUID | PK, not null |
| user_id | UUID | FK → User, not null |
| project_id | UUID | FK → Project, not null |
| role | ENUM | not null — `owner \| admin \| member` |
| status | ENUM | not null — `invited \| active` |
| created_at | TIMESTAMP | not null |

**Restrições:**
- `(user_id, project_id)`: unique — um usuário tem apenas um vínculo por projeto
- Um projeto deve ter exatamente 1 `owner` ativo

---

### GitHubAccount

| Campo | Tipo | Restrições |
|---|---|---|
| id | UUID | PK, not null |
| project_id | UUID | FK → Project, not null |
| github_id | BIGINT | unique, not null — ID externo do GitHub |
| account_type | ENUM | not null — `user \| org` |
| username | VARCHAR(100) | not null |
| profile_url | VARCHAR(255) | not null |
| access_token | TEXT | not null, armazenado encriptado |
| created_at | TIMESTAMP | not null |

**Validações:**
- `access_token`: nunca exposto em responses — apenas usado internamente pelo backend
- Um `project_id` pode ter apenas 1 conta GitHub vinculada por vez

---

### Server

Unifica o conceito de servidor e ambiente. Um servidor pertence a exatamente um ambiente do projeto.

| Campo | Tipo | Restrições |
|---|---|---|
| id | UUID | PK, not null |
| project_id | UUID | FK → Project, not null |
| environment | ENUM | not null — `dev \| hom \| prod` |
| agent_token | VARCHAR(255) | unique, not null, armazenado como hash |
| status | ENUM | not null — `active \| inactive` |
| last_seen_at | TIMESTAMP | nullable — atualizado via heartbeat |
| created_at | TIMESTAMP | not null |

**Validações:**
- `environment`: obrigatório — servidor só pode ser criado associado a um ambiente válido
- `agent_token`: gerado pela plataforma, nunca regerado automaticamente — apenas sob ação explícita do usuário
- `status` calculado para exibição: se `last_seen_at` > 5 min atrás → `offline`, caso contrário → `online`

---