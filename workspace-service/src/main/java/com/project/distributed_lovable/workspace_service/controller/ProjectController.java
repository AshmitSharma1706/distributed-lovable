package com.project.distributed_lovable.workspace_service.controller;

import com.project.distributed_lovable.workspace_service.dto.deploy.DeployResponse;
import com.project.distributed_lovable.workspace_service.dto.project.ProjectRequest;
import com.project.distributed_lovable.workspace_service.dto.project.ProjectResponse;
import com.project.distributed_lovable.workspace_service.dto.project.ProjectSummaryResponse;
import com.project.distributed_lovable.workspace_service.service.DeploymentService;
import com.project.distributed_lovable.workspace_service.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final DeploymentService deploymentService;

    @PostMapping("create")
    public ResponseEntity<ProjectResponse> createProject(@RequestBody @Valid ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
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

















