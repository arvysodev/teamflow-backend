package com.teamflow.teamflow.backend.tasks.repo;

import com.teamflow.teamflow.backend.tasks.domain.Task;
import com.teamflow.teamflow.backend.tasks.domain.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Page<Task> findAllByProjectId(UUID projectId, Pageable pageable);

    Page<Task> findAllByProjectIdAndStatus(UUID projectId, TaskStatus status, Pageable pageable);

    Optional<Task> findByIdAndProjectId(UUID id, UUID projectId);
}
