package com.project.distributed_lovable.workspace_service.dto.member;

import com.project.distributed_lovable.common_lib.enums.ProjectRole;

import java.time.Instant;

public record MemberResponse(
        Long userId,
        String email,
        String name,
        ProjectRole role,
        Instant invitedAt
) {
}
