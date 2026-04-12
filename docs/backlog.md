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
- Criar projeto (gera slug único a partir do name)
- Gerenciar membros (convidar, remover, alterar role)
- Associar conta GitHub via OAuth
- Gerenciar servidores

**Regras**
- 1 projeto → 1 conta GitHub
- Repositórios devem pertencer a essa conta
- Apenas `owner` e `admin` podem associar GitHub e criar servidores
- Isolamento total por `project_id`
- GitHub OAuth: permissões mínimas necessárias — `repo`

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
