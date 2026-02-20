CREATE TABLE projects (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          workspace_id UUID NOT NULL,
                          name VARCHAR(255) NOT NULL,
                          status VARCHAR(20) NOT NULL,
                          created_by UUID NOT NULL,
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP NOT NULL,

                          CONSTRAINT fk_projects_workspace
                              FOREIGN KEY (workspace_id) REFERENCES workspaces(id)
);

CREATE UNIQUE INDEX uq_projects_workspace_name ON projects(workspace_id, name);
CREATE INDEX idx_projects_workspace_id ON projects(workspace_id);
CREATE INDEX idx_projects_workspace_status ON projects(workspace_id, status);
