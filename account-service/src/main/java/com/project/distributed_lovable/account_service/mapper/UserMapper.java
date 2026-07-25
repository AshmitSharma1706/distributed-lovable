package com.project.distributed_lovable.account_service.mapper;

import com.project.distributed_lovable.account_service.dto.auth.SignupRequest;
import com.project.distributed_lovable.account_service.dto.auth.UserProfileResponse;
import com.project.distributed_lovable.account_service.entity.User;
import com.project.distributed_lovable.common_lib.dto.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(SignupRequest signupRequest);
    UserProfileResponse toUserProfileResponse(User user);
    UserDto toUserDto(User user);
}
