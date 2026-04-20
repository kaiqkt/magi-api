CREATE TABLE users
(
    id            VARCHAR(26)  NOT NULL,
    email         VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE projects
(
    id         VARCHAR(26) NOT NULL,
    name       VARCHAR(50) NOT NULL,
    tenant_id  VARCHAR(50) NOT NULL,
    created_by VARCHAR(26) NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    CONSTRAINT pk_projects PRIMARY KEY (id),
    CONSTRAINT uq_projects_tenant_id UNIQUE (tenant_id),
    CONSTRAINT fk_projects_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE project_memberships
(
    id         VARCHAR(26) NOT NULL,
    user_id    VARCHAR(26) NOT NULL,
    project_id VARCHAR(26) NOT NULL,
    role       VARCHAR(20) NOT NULL,
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    CONSTRAINT pk_project_memberships PRIMARY KEY (id),
    CONSTRAINT uq_project_memberships_user_project UNIQUE (user_id, project_id),
    CONSTRAINT fk_project_memberships_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_project_memberships_project FOREIGN KEY (project_id) REFERENCES projects (id)
);

CREATE TABLE git_accounts
(
    id           VARCHAR(26)  NOT NULL,
    project_id   VARCHAR(26)  NOT NULL,
    account_type VARCHAR(10)  NOT NULL,
    username     VARCHAR(100) NOT NULL,
    profile_url  VARCHAR(255) NOT NULL,
    access_token TEXT         NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    CONSTRAINT pk_github_accounts PRIMARY KEY (id),
    CONSTRAINT uq_github_accounts_project UNIQUE (project_id),
    CONSTRAINT fk_github_accounts_project FOREIGN KEY (project_id) REFERENCES projects (id)
);

CREATE TABLE servers
(
    id           VARCHAR(26)  NOT NULL,
    project_id   VARCHAR(26)  NOT NULL,
    environment  VARCHAR(10)  NOT NULL,
    agent_token  VARCHAR(255) NOT NULL,
    status       VARCHAR(10)  NOT NULL,
    last_seen_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL,
    CONSTRAINT pk_servers PRIMARY KEY (id),
    CONSTRAINT uq_servers_agent_token UNIQUE (agent_token),
    CONSTRAINT fk_servers_project FOREIGN KEY (project_id) REFERENCES projects (id)
);

CREATE TABLE user_roles
(
    user_id VARCHAR(26) NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    roles   VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, roles)
);

CREATE TABLE applications
(
    id             VARCHAR(26)  NOT NULL,
    name           VARCHAR(50)  NOT NULL,
    status         VARCHAR(25)  NOT NULL,
    description    VARCHAR(255),
    repository_url VARCHAR(255) NOT NULL,
    project_id     VARCHAR(26)  NOT NULL,
    update_at      TIMESTAMP,
    created_at     TIMESTAMP    NOT NULL,
    CONSTRAINT fk_applications_project FOREIGN KEY (project_id) REFERENCES projects (id)
);
