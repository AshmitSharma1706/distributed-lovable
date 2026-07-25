package com.project.distributed_lovable.common_lib.enums;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Set;

import static com.project.distributed_lovable.common_lib.enums.ProjectPermission.*;


@RequiredArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ProjectRole {
    EDITOR(VIEW, EDIT, DELETE, VIEW_MEMBER),
    VIEWER(VIEW, VIEW_MEMBER),
    OWNER(Set.of(VIEW, EDIT, DELETE, VIEW_MEMBER, MANAGE_MEMBER));

    ProjectRole(ProjectPermission... permissions) {
        this.permissions = Set.of(permissions);
    }

    Set<ProjectPermission> permissions;
}
