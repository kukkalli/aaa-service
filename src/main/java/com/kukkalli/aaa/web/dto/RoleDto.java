package com.kukkalli.aaa.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "Role")
public record RoleDto(
        Long id,
        @Schema(example = "ROLE_ADMIN") String code,
        @Schema(example = "Administrator") String name,
        String description,
        // Convenience: include permissions (codes) attached to this role
        List<String> permissions,
        Instant createdAt,
        Instant updatedAt
) {}
