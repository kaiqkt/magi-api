# Magi — Backlog

## v1 — Escopo atual

### Conta e Autenticação

**Features**
- Criar conta
- Login / Logout com controle de sessão
- Recuperação de senha

**Regras**
- Convite gera `membership` com status `invited` — aceite ativa o vínculo
- Remoção de usuário do projeto não deleta a conta global
- Um usuário pode participar de múltiplos projetos

---

### Projeto

**Features**
- Criar projeto (gera tenant id único a partir do name)
- Gerenciar membros (convidar, remover, alterar role)
- Associar conta GitHub (receber e validar access token)
- Gerenciar servidores

**Regras**
- 1 projeto → 1 conta GitHub
- Repositórios devem pertencer a essa conta
- Apenas `owner` e `admin` podem associar GitHub e criar servidores
- Isolamento total por `project_id`
- GitHub OAuth é responsabilidade da `api-cli` — ela executa o fluxo completo e entrega o access token ao Magi
- Magi recebe o access token e valida que ele é válido antes de persistir
- Permissões mínimas necessárias no token — `repo`

---

### Servidores e Agent

**Features**
- Criar servidor associado a um environment (dev | hom | prod)
- Gerar `agent_token` — exibido uma única vez
- Exibir instruções de instalação do agent
- Ativar / desativar servidor
- Status online/offline via `last_seen_at` (heartbeat)

**Regras**
- A plataforma nunca acessa diretamente os servidores
- Agent autentica via `agent_token` e faz polling de eventos
- `agent_token` rotacionável apenas sob ação explícita do usuário
- Eventos precisam ser idempotentes (seguros para reexecução)

**Polling — comportamento esperado**
```
GET /agents/{server_id}/events
→ retorna eventos pendentes
→ agent executa
→ agent confirma execução
```

---

## Backlog / Radar

### API Gateway

Cada projeto terá uma instância de API Gateway na frente da Magi API:
- Rotas **públicas**: sem autenticação
- Rotas **internas**: apenas serviços dentro da rede (ex: agents)
- Rotas **protegidas**: requerem autenticação do usuário

```
Internet ──► API Gateway ──► Magi API
                 │
          (policy de rotas por projeto)
```

> Tecnologia ainda não definida (Kong, Traefik, custom). A API deve ser desenhada considerando este layer.

**Evolução do tenant resolution:**

Atualmente o `tenantId` é extraído diretamente do subdomínio do `Host` header no próprio backend (`TenantFilter`). Isso é temporário.

A mudança planejada:
- O **Gateway** intercepta o subdomínio, resolve para um tenant ID real e o injeta como header na requisição para o backend
- O backend passa a confiar no header vindo do Gateway em vez de fazer a extração do host
- O campo `tenantId` no `Project` volta a ser apenas o **slug** (ex: `"my-project"`)
- Um **ID dedicado** será criado especificamente para identificar o tenant (separado do slug e do `project.id`)

```
Client → Gateway (resolve host → tenantId real) → Magi API (lê tenantId do header)
```

Impacto no código: `TenantFilter` e `TenantContext` serão substituídos por leitura de header; o model `Project` ganhará um campo `tenantId` explícito como identificador único do tenant.

---

### Catálogo de Eventos (Agent)

A detalhar quando o escopo de deploy for refinado.

**Plataforma → Agent (comandos)**

| Tipo | Descrição |
|---|---|
| `DEPLOY` | Solicita deploy de uma aplicação |
| *(a definir)* | |

**Agent → Plataforma (respostas)**

| Tipo | Descrição |
|---|---|
| `HEARTBEAT` | Sinal de vida do agent |
| `EVENT_ACK` | Confirmação de recebimento |
| `EVENT_RESULT` | Resultado da execução (sucesso/falha) |
| *(a definir)* | |

---

### Comunicação Magi ↔ Agent — Evolução

**v1:** Long polling — agent faz `GET /agents/{id}/events`, servidor segura a conexão até ter evento ou timeout (~30s). Sem infraestrutura extra.

**Futuro:** Migrar para WebSocket com Redis Pub/Sub para suportar escala horizontal:

```
Infraestrutura Magi
┌──────────────────────────────────────────┐
│  Magi Instance 1 ──┐                     │
│  Magi Instance 2 ──┼──► Redis Pub/Sub    │
│  Magi Instance N ──┘                     │
└──────────────────────────────────────────┘
        ▲                   ▲
        │ WebSocket         │ WebSocket
   Agent (srv A)       Agent (srv B)
```

- Agent abre conexão WebSocket com o Magi — não conhece Redis
- Redis é interno ao Magi: garante que o evento chegue na instância que tem a conexão ativa do agent
- Necessário apenas quando Magi rodar com mais de uma instância

---

### Deploy e Infraestrutura

- Gerenciamento de deploy (pipelines, histórico)
- Deploy de serviços (banco de dados, filas, etc.)
- Métricas e logs vindos do agent
- Comunicação entre servidores (caso multi-server por projeto)
