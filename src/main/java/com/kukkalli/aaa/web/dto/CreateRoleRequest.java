package com.kukkalli.aaa.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(name = "CreateRoleRequest", description = "Payload to create a new role")
public record CreateRoleRequest(

        @NotBlank
        @Size(max = 64)
        @Schema(description = "Unique role code (e.g., ROLE_ADMIN, ROLE_USER)", example = "ROLE_AUDITOR")
        String code,

        @NotBlank
        @Size(max = 128)
        @Schema(description = "Display name", example = "Auditor")
        String name,

        @Nullable
        @Size(max = 512)
        @Schema(description = "Optional description", example = "Read-only access to audit logs")
        String description,

        @Schema(description = "Permission codes to assign to the role",
                example = "[\"audit.read\",\"user.read\"]")
        List<@Size(max = 128) String> permissions
) {}
