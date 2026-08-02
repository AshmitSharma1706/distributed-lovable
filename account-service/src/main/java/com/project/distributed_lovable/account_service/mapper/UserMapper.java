package com.project.distributed_lovable.account_service.mapper;

import com.project.distributed_lovable.account_service.dto.auth.SignupRequest;
import com.project.distributed_lovable.account_service.dto.auth.UserProfileResponse;
import com.project.distributed_lovable.account_service.entity.User;
import com.project.distributed_lovable.common_lib.dto.UserDto;
import com.project.distributed_lovable.common_lib.security.JwtUserPrincipal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(SignupRequest signupRequest);

    @Mapping(source = "userId", target = "id")
    UserProfileResponse toUserProfileResponse(JwtUserPrincipal user);

    UserDto toUserDto(User user);
}
