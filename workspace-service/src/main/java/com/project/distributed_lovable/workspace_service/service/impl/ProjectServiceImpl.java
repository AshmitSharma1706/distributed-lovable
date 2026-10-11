package com.project.distributed_lovable.workspace_service.service.impl;


import com.project.distributed_lovable.common_lib.dto.PlanDto;
import com.project.distributed_lovable.common_lib.enums.ProjectPermission;
import com.project.distributed_lovable.common_lib.enums.ProjectRole;
import com.project.distributed_lovable.common_lib.error.BadRequestException;
import com.project.distributed_lovable.common_lib.error.ResourceNotFoundException;
import com.project.distributed_lovable.common_lib.security.AuthUtil;
import com.project.distributed_lovable.workspace_service.client.AccountClient;
import com.project.distributed_lovable.workspace_service.dto.project.ProjectRequest;
import com.project.distributed_lovable.workspace_service.dto.project.ProjectResponse;
import com.project.distributed_lovable.workspace_service.dto.project.ProjectSummaryResponse;
import com.project.distributed_lovable.workspace_service.entity.Project;
import com.project.distributed_lovable.workspace_service.entity.ProjectMember;
import com.project.distributed_lovable.workspace_service.entity.ProjectMemberId;
import com.project.distributed_lovable.workspace_service.mapper.ProjectMapper;
import com.project.distributed_lovable.workspace_service.repository.ProjectMemberRepository;
import com.project.distributed_lovable.workspace_service.repository.ProjectRepository;
import com.project.distributed_lovable.workspace_service.security.SecurityExpressions;
import com.project.distributed_lovable.workspace_service.service.ProjectService;
import com.project.distributed_lovable.workspace_service.service.ProjectTemplateService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Transactional
public class ProjectServiceImpl implements ProjectService {

    ProjectRepository projectRepository;
    ProjectMapper projectMapper;
    ProjectMemberRepository projectMemberRepository;
    AuthUtil authUtil;
    ProjectTemplateService projectTemplateService;
    AccountClient accountClient;
    SecurityExpressions securityExpressions;

    private static final int DEFAULT_FREE_TIER_MAX_PROJECTS = 100;

    @Override
    public ProjectResponse createProject(ProjectRequest request) {
        Long ownerUserId = authUtil.getCurrentUserId();
        log.info("Project creation started: userId={}, projectName='{}'", ownerUserId, request.name());

        if (!canCreateProject()) {
            log.warn("Project creation rejected for userId={}: project limit reached for plan", ownerUserId);
            throw new BadRequestException("User cannot create a new project with current plan, Upgrade your plan now...");
        }

        Project project = Project.builder()
                .name(request.name())
                .isPublic(false)
                .build();

        log.debug("Saving project to PostgreSQL database: name='{}'", request.name());
        project = projectRepository.save(project);
        log.info("Project entity saved successfully: projectId={}, name='{}'", project.getId(), project.getName());

        ProjectMemberId projectMemberId = new ProjectMemberId(project.getId(), ownerUserId);
        ProjectMember projectMember = ProjectMember.builder()
                .id(projectMemberId)
                .projectRole(ProjectRole.OWNER)
                .invitedAt(Instant.now())
                .acceptedAt(Instant.now())
                .project(project)
                .build();
        projectMemberRepository.save(projectMember);
        log.debug("Project owner member record saved: projectId={}, ownerUserId={}", project.getId(), ownerUserId);

        log.info("Initializing project files from template for projectId={}", project.getId());
        projectTemplateService.initializeProjectFromTemplate(project.getId());
        log.info("Project files initialized for projectId={}", project.getId());

        ProjectResponse response = projectMapper.toProjectResponse(project);
        log.info("Project creation completed successfully: projectId={}, name='{}', userId={}",
                response.id(), response.name(), ownerUserId);
        return response;
    }

    @Override
    public List<ProjectSummaryResponse> getUserProjects() {
        Long userId= authUtil.getCurrentUserId();
        var projectsWithRoles=projectRepository.findAllAccessibleByUser(userId);
        return projectsWithRoles.stream()
                .map(p -> projectMapper.toProjectSummaryResponse(p.getProject(), p.getRole()))
                .toList();
    }

    @Override
    @PreAuthorize("@security.canViewProject(#projectId)")
    public ProjectSummaryResponse getUserProjectById(Long projectId) {
        Long userId = authUtil.getCurrentUserId();

        var projectWithRole = projectRepository.findAccessibleProjectByIdWithRole(projectId, userId)
                .orElseThrow(() -> new BadRequestException("Project Not Found"));

        return projectMapper.toProjectSummaryResponse(projectWithRole.getProject(), projectWithRole.getRole());
    }

    @Override
    @PreAuthorize("@security.canEditProject(#projectId)")
    public ProjectResponse updateProject(Long projectId, ProjectRequest request) {
        Long userId= authUtil.getCurrentUserId();
        Project project=projectRepository.findAccessibleProjectById(projectId,userId).orElseThrow();
        project.setName(request.name());
        project=projectRepository.save(project);
        return projectMapper.toProjectResponse(project);
    }

    @Override
    @PreAuthorize("@security.canDeleteProject(#projectId)")
    public void softDelete(Long projectId) {
        Long userId= authUtil.getCurrentUserId();
        Project project= getAccessibleProjectById(projectId, userId);
        project.setDeletedAt(Instant.now());
        projectRepository.save(project);
    }

    @Override
    public boolean hasPermission(Long projectId, ProjectPermission permission) {
        return securityExpressions.hasPermission(projectId, permission);
    }

    public Project getAccessibleProjectById(Long projectId, Long userId){
        return projectRepository.findAccessibleProjectById(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));
    }

    private boolean canCreateProject() {
        Long userId = authUtil.getCurrentUserId();
        if (userId == null) {
            log.warn("canCreateProject failed: userId is null");
            return false;
        }

        int maxAllowed = DEFAULT_FREE_TIER_MAX_PROJECTS;
        try {
            PlanDto plan = accountClient.getCurrentSubscribedPlanByUser();
            if (plan != null && plan.maxProjects() != null) {
                maxAllowed = plan.maxProjects();
                log.debug("Fetched subscription plan for userId={}: planName='{}', maxProjects={}",
                        userId, plan.name(), maxAllowed);
            } else {
                log.info("No active paid subscription for userId={}, defaulting to free tier limit: {}",
                        userId, maxAllowed);
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve plan from account-service for userId={}: {}. Falling back to default limit: {}",
                    userId, e.getMessage(), maxAllowed);
        }

        int ownedCount = projectMemberRepository.countProjectOwnedByUser(userId);
        log.info("Project limit check: userId={}, ownedCount={}, maxAllowed={}", userId, ownedCount, maxAllowed);

        return ownedCount < maxAllowed;
    }
}
    