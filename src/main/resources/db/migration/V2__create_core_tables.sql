CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE workspaces (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            name VARCHAR(255) NOT NULL,
                            created_at TIMESTAMP NOT NULL
);

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workspace_members (
                                   workspace_id UUID NOT NULL,
                                   user_id UUID NOT NULL,
                                   role VARCHAR(50) NOT NULL,
                                   joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT fk_workspace
                                       FOREIGN KEY (workspace_id)
                                           REFERENCES workspaces(id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT fk_user
                                       FOREIGN KEY (user_id)
                                           REFERENCES users(id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT workspace_member_unique
                                       UNIQUE (workspace_id, user_id)
);

CREATE INDEX idx_workspace_members_workspace
    ON workspace_members(workspace_id);

CREATE INDEX idx_workspace_members_user
    ON workspace_members(user_id);
