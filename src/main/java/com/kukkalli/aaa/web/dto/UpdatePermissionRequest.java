package com.kukkalli.aaa.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Size;

@Schema(name = "UpdatePermissionRequest")
public record UpdatePermissionRequest(
        @Nullable @Size(max = 128) String name,
        @Nullable @Size(max = 512) String description
) {}
