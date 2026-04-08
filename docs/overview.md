# Magi — Overview

## Problema

Gerenciar deploys e infraestrutura de aplicações é complexo e fragmentado. Equipes pequenas precisam lidar com múltiplas ferramentas, acesso direto a servidores via SSH, configuração manual de conexões entre serviços (bancos, filas, etc.) e ausência de rastreabilidade sobre o que foi feito e onde.

## Solução

Magi é uma plataforma de gerenciamento de deploy e infraestrutura que abstrai essa complexidade. O usuário gerencia tudo pela plataforma — servidores, aplicações, ambientes — sem precisar de acesso direto aos servidores. A comunicação com os servidores é feita por um **agent** que roda dentro de cada servidor e se comunica com a plataforma via polling.

Referência de inspiração: [Coolify](https://coolify.io)

---

## Conceitos Core

| Conceito | Descrição |
|---|---|
| **Account** | Identidade global do usuário na plataforma |
| **Project** | Tenant isolado. Agrupa membros, servidores e aplicações |
| **Membership** | Vínculo entre usuário e projeto (owner / admin / member) |
| **Server** | Servidor registrado no projeto. Identificado por environment |
| **Agent** | Processo que roda no servidor. Autentica via `agent_token` e faz polling de eventos |
| **GitHub Account** | Integração OAuth. Associada ao projeto para acesso a repositórios |

---

## Arquitetura Geral

```
                        ┌─────────────┐
                        │    User     │
                        └──────┬──────┘
                               │ HTTP
                        ┌──────▼──────┐
                        │  Magi API   │
                        └──────┬──────┘
                               │ polling (pull)
              ┌────────────────┼────────────────┐
              │                │                │
       ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
       │   Agent     │  │   Agent     │  │   Agent     │
       │  (dev srv)  │  │  (hom srv)  │  │  (prod srv) │
       └──────┬──────┘  └──────┬──────┘  └──────┴──────┘
              │                │
        executa localmente no servidor
        (deploy, config, etc.)
```

**Princípio importante:** A plataforma nunca acessa diretamente os servidores. São os agents que iniciam o contato, buscando eventos pendentes.

---

## Fluxo principal — Registro de servidor

```
User                  Magi API              Server
 │                        │                    │
 │  cria servidor         │                    │
 │───────────────────────>│                    │
 │                        │                    │
 │<── agent_token ────────│                    │
 │                        │                    │
 │  instala agent + token no servidor          │
 │────────────────────────────────────────────>│
 │                        │                    │
 │                        │<── polling (GET /agents/{id}/events)
 │                        │                    │
 │                        │─── eventos ───────>│
 │                        │                    │ executa
 │                        │<── confirmação ────│
```

---

## Escopo v1

### Incluído
- Conta e autenticação (login, logout, recuperação de senha)
- Projetos (criação, membros, ambientes)
- Integração com GitHub (OAuth, token por projeto)
- Servidores (cadastro, geração de `agent_token`, status via heartbeat)
- Agent (polling de eventos, heartbeat)

### Fora do escopo v1
- Gerenciamento de deploy (pipelines, histórico)
- Deploy de serviços (banco de dados, filas, etc.)
- Métricas e logs vindos do agent
- Comunicação entre servidores
- API Gateway (registrado no backlog)

> Ver [backlog.md](backlog.md) para itens detalhados fora do v1.

---

## Decisões em aberto

- [ ] Repositório do agent: separado ou monorepo?
- [ ] Estrutura do payload dos eventos (schema)
- [ ] Estratégia de rotação do `agent_token`
- [ ] Tecnologia do API Gateway (Kong, Traefik, custom)
