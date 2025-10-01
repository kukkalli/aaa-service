package com.kukkalli.aaa.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(name = "UpdateUserRequest")
public record UpdateUserRequest(
        @Nullable @Email @Size(max = 191) String email,
        @Nullable @Size(max = 100) String firstName,
        @Nullable @Size(max = 100) String lastName,
        @Nullable @Size(max = 40)  String phone,

        @Nullable Boolean enabled,
        @Nullable Boolean accountNonLocked,
        @Nullable Boolean accountNonExpired,
        @Nullable Boolean credentialsNonExpired,

        @Schema(description = "Replace roles with these codes if provided")
        @Nullable List<String> roles
) {}
