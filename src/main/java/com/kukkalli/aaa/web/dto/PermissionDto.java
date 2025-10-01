package com.kukkalli.aaa.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "Permission")
public record PermissionDto(
        Long id,
        @Schema(example = "user.read") String code,
        @Schema(example = "Read Users") String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
