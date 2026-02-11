ALTER TABLE workspaces
    ADD CONSTRAINT uq_workspaces_name UNIQUE (name);