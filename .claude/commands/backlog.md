# Skill: backlog

Creates and manages tasks manually in the project's local backlog following a hierarchical structure of features and contexts.

> For bulk task generation from a system design document, use `/system-design tasks [topic]` instead.

## Usage

```
/backlog [user story or task description]
/backlog list
/backlog status [task title] [new-status]
```

## Behavior

When the command is `/backlog list`, run the **List** flow.
When the command starts with `/backlog status`, run the **Status** flow.
Otherwise, run the **Create** flow.

---

### List flow

Read all `tasks.md` files under `docs/backlog/` and output a single markdown table:

| Feature | Context | Title | Status |
|---------|---------|-------|--------|

- **Feature** and **Context** are derived from the file path (`docs/backlog/{feature}/{context}/tasks.md`).
- **Title** is each `## [Task Title]` heading found in the file.
- **Status** is the value of the `**Status:**` field for that task.
- Sort rows by Feature → Context → order of appearance in the file.
- If `docs/backlog/` does not exist or contains no tasks, respond with: `No tasks found.`

---

### Status flow

Updates the status of an existing task.

**Valid statuses:** `todo`, `in-progress`, `done`

#### Steps

1. Search all `tasks.md` files under `docs/backlog/` for a `## [Task Title]` heading that matches (case-insensitive, partial match allowed).
2. If no task is found, respond with: `Task not found.`
3. If multiple tasks match, list them with their feature/context path and ask the user to be more specific.
4. Replace the `**Status:**` value for the matched task with the new status.
5. Update `docs/backlog/index.md` — recount todos, in-progress, and done for the affected context and update the corresponding row in the `Overview` table.

---

### Create flow

When receiving a request, the agent must:

### 1. Collect information

If the user does not provide all required data, ask for:
- **Feature**: what is the main grouping? (e.g., `backend`, `frontend`, `infra`)
- **Context**: what is the subdirectory within the feature? Can be hierarchical (e.g., `auth`, `data-modeling`, `payments/pix`)
- **Task title**: short and descriptive name
- **Description**: details of what needs to be done
- **User Story**: in the format "As a [user], I want [action] so that [benefit]."

If the user already provides a user story in the request, derive the title and description from it. Do not ask for information already provided.

### 2. Directory structure

The backlog always lives in `/docs/backlog/` relative to the project root.

```
docs/backlog/
  index.md
  {feature}/
    {context}/
      tasks.md
```

Valid examples:
- `docs/backlog/backend/auth/tasks.md`
- `docs/backlog/backend/data-modeling/tasks.md`
- `docs/backlog/frontend/dashboard/tasks.md`
- `docs/backlog/backend/payments/pix/tasks.md`

Create any directories that do not exist.

### 3. Task format

Each `tasks.md` groups tasks for the context. When adding a new task, follow this format:

```markdown
## [Task Title]

**Status:** `todo`
**Description:** [Objective description of what needs to be implemented]
**User Story:** As a [user], I want [action] so that [benefit].
```

- The initial status is always `todo`
- If the `tasks.md` file does not exist, create it with the following header before the tasks:

```markdown
# [Context Name]

> Feature: [feature name]
```

- If it already exists, just append the new task to the end of the file.

### 4. Update index.md

The `docs/backlog/index.md` file is the backlog overview. It must always reflect the current state.

If it does not exist, create it with this structure:

```markdown
# Backlog

> General index of project features and tasks.

## Overview

| Feature | Context | Todo | In Progress | Done |
|---------|---------|------|-------------|------|

## Features

```

When adding or updating a task:
1. Read the context's `tasks.md` to count tasks by status
2. Update (or insert) the corresponding row in the `Overview` table
3. If the feature does not yet appear in the `## Features` section, add a link:

```markdown
### [feature]
- [context](feature/context/tasks.md)
```

### 5. General rules

- Directory names in **kebab-case** and **lowercase** (e.g., `data-modeling`, `auth`)
- Never overwrite existing tasks — only append
- Always update `index.md` after creating or editing any `tasks.md`
- If the user story is vague, keep it as provided — do not invent details
