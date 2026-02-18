CREATE TABLE workspace_invites (
                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                   workspace_id UUID NOT NULL,
                                   email VARCHAR(255) NOT NULL,

                                   token_hash VARCHAR(255) NOT NULL UNIQUE,

                                   expires_at TIMESTAMP NOT NULL,
                                   accepted_at TIMESTAMP NULL,

                                   created_by UUID NOT NULL,
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE workspace_invites
    ADD CONSTRAINT fk_workspace_invites_workspace
        FOREIGN KEY (workspace_id)
            REFERENCES workspaces(id)
            ON DELETE CASCADE;

ALTER TABLE workspace_invites
    ADD CONSTRAINT fk_workspace_invites_created_by
        FOREIGN KEY (created_by)
            REFERENCES users(id)
            ON DELETE RESTRICT;

CREATE INDEX idx_workspace_invites_workspace_id
    ON workspace_invites(workspace_id);

CREATE INDEX idx_workspace_invites_email
    ON workspace_invites(email);

CREATE INDEX idx_workspace_invites_expires_at
    ON workspace_invites(expires_at);

CREATE INDEX idx_workspace_invites_created_by
    ON workspace_invites(created_by);
