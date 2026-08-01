package com.project.distributed_lovable.workspace_service.service;



import com.project.distributed_lovable.workspace_service.dto.project.ProjectRequest;
import com.project.distributed_lovable.workspace_service.dto.project.ProjectResponse;
import com.project.distributed_lovable.workspace_service.dto.project.ProjectSummaryResponse;

import java.util.List;

public interface ProjectService {
    List<ProjectSummaryResponse> getUserProjects();

    ProjectSummaryResponse getUserProjectById(Long projectId);

    ProjectResponse createProject(ProjectRequest request);

    ProjectResponse updateProject(Long projectId, ProjectRequest request);

    void softDelete(Long projectId);
}
