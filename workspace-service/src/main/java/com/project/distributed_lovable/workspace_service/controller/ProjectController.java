package com.project.distributed_lovable.workspace_service.controller;

import com.project.distributed_lovable.workspace_service.dto.deploy.DeployResponse;
import com.project.distributed_lovable.workspace_service.dto.project.ProjectRequest;
import com.project.distributed_lovable.workspace_service.dto.project.ProjectResponse;
import com.project.distributed_lovable.workspace_service.dto.project.ProjectSummaryResponse;
import com.project.distributed_lovable.workspace_service.service.DeploymentService;
import com.project.distributed_lovable.workspace_service.service.ProjectService;
import com.project.distributed_lovable.common_lib.security.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final DeploymentService deploymentService;
    private final AuthUtil authUtil;

    @PostMapping("/create")
    public ResponseEntity<ProjectResponse> createProject(@RequestBody @Valid ProjectRequest request) {
        Long userId = authUtil.getCurrentUserId();
        log.info("Project creation request received: name='{}', authenticatedUserId={}", request.name(), userId);
        ProjectResponse response = projectService.createProject(request);
        log.info("Project created successfully: projectId={}, name='{}', userId={}", response.id(), response.name(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjectSummaryResponse>> getMyProjects() {
        return ResponseEntity.ok(projectService.getUserProjects());
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectSummaryResponse> getProjectById(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getUserProjectById(projectId));
    }

    @PatchMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable Long projectId, @RequestBody @Valid ProjectRequest request) {
        return ResponseEntity.ok(projectService.updateProject(projectId, request));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId) {
        projectService.softDelete(projectId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{projectId}/deploy")
    public ResponseEntity<DeployResponse> deployProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(deploymentService.deploy(projectId));
    }

}

















