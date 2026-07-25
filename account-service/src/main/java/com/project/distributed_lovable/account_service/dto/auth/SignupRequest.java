package com.project.distributed_lovable.account_service.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Email
        @NotBlank
        String username,

        @NotBlank
        @Size(min = 3, max = 20)
        String name,

        @NotBlank
        @Size(min = 8, max = 20)
        String password
) {
}
