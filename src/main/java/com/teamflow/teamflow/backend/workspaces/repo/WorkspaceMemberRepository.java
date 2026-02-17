package com.teamflow.teamflow.backend.workspaces.repo;

import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceMember;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, WorkspaceMemberId> {

}
