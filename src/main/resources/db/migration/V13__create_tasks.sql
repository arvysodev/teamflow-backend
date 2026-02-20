CREATE TABLE tasks (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       project_id UUID NOT NULL,
                       title VARCHAR(255) NOT NULL,
                       description TEXT,
                       status VARCHAR(20) NOT NULL,
                       assignee_user_id UUID,
                       created_by UUID NOT NULL,
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP NOT NULL,

                       CONSTRAINT fk_tasks_project
                           FOREIGN KEY (project_id) REFERENCES projects(id),

                       CONSTRAINT fk_tasks_assignee_user
                           FOREIGN KEY (assignee_user_id) REFERENCES users(id),

                       CONSTRAINT fk_tasks_created_by_user
                           FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_tasks_project_id ON tasks(project_id);
CREATE INDEX idx_tasks_project_status ON tasks(project_id, status);
CREATE INDEX idx_tasks_project_assignee ON tasks(project_id, assignee_user_id);
