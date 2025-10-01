package com.kukkalli.aaa.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "CreatePermissionRequest")
public record CreatePermissionRequest(

        @NotBlank @Size(max = 128)
        @Schema(example = "user.read")
        String code,

        @NotBlank @Size(max = 128)
        @Schema(example = "Read Users")
        String name,

        @Size(max = 512)
        @Schema(example = "Allows reading user profiles")
        String description
) {}
