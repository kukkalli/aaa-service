package com.kukkalli.aaa.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(name = "UpdateRoleRequest", description = "Payload to update an existing role")
public record UpdateRoleRequest(

        @Nullable
        @Size(max = 128)
        @Schema(description = "Display name", example = "Auditor")
        String name,

        @Nullable
        @Size(max = 512)
        @Schema(description = "Optional description", example = "Read-only access to audit logs")
        String description,

        @Nullable
        @Schema(description = "Replace permissions with this set of codes",
                example = "[\"audit.read\",\"user.read\"]")
        List<@Size(max = 128) String> permissions
) {}
