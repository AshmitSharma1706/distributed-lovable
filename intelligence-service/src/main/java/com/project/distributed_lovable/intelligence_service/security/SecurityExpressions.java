package com.project.distributed_lovable.intelligence_service.security;

import com.project.distributed_lovable.common_lib.enums.ProjectPermission;
import com.project.distributed_lovable.common_lib.security.AuthUtil;
import com.project.distributed_lovable.intelligence_service.client.WorkspaceClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component("security")
@RequiredArgsConstructor
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
        return workspaceClient.checkPermission(projectId, permission);
    }
}
