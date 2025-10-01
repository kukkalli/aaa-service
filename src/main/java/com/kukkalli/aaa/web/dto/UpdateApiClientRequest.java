package com.kukkalli.aaa.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(name = "UpdateApiClientRequest", description = "Payload to update an API client")
public record UpdateApiClientRequest(

        @Nullable
        @Size(max = 191)
        @Schema(description = "Updated display name/label", example = "New Analytics Service")
        String name,

        @Nullable
        @Schema(description = "Updated scopes (replace existing)",
                example = "[\"user.read\",\"user.write\"]")
        List<@Size(max = 128) String> scopes,

        @Nullable
        @Schema(description = "Updated allowed IPs/CIDRs (replace existing)",
                example = "[\"192.168.1.0/24\"]")
        List<@Size(max = 64) String> allowedIps,

        @Nullable
        @Schema(description = "Enable/disable the client", example = "true")
        Boolean enabled
) {}
