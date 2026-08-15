package com.project.distributed_lovable.intelligence_service.security;

import com.project.distributed_lovable.common_lib.enums.ProjectPermission;
import com.project.distributed_lovable.common_lib.security.AuthUtil;
import com.project.distributed_lovable.intelligence_service.client.WorkspaceClient;
import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.stereotype.Component;

@Component("security")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityExpressions {
    AuthUtil authUtil;
    WorkspaceClient workspaceClient;

    public boolean canViewProject(Long projectId){
        return hasPermission(projectId, ProjectPermission.VIEW);
    }

    public boolean canEditProject(Long projectId){
        return hasPermission(projectId, ProjectPermission.EDIT);
    }

    public boolean canDeleteProject(Long projectId){
        return hasPermission(projectId, ProjectPermission.DELETE);
    }

    public boolean canViewMember(Long projectId){
        return hasPermission(projectId, ProjectPermission.VIEW_MEMBER);
    }

    public boolean canManageMember(Long projectId){
        return hasPermission(projectId, ProjectPermission.MANAGE_MEMBER);
    }

    private boolean hasPermission(Long projectId, ProjectPermission permission){
        try {
            return workspaceClient.checkPermission(projectId, permission);
        } catch (FeignException.Unauthorized e) {
            log.warn("Token expired or invalid during permission check for project: {}", projectId);
            throw new CredentialsExpiredException("JWT token is expired or invalid");
        } catch (FeignException e) {
            log.error("Workspace-service failed during permission check: {}", e.getMessage());
            return false;
        }
    }
}
