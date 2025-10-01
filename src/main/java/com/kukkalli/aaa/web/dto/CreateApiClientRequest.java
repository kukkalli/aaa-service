package com.kukkalli.aaa.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(name = "CreateApiClientRequest", description = "Payload to create a new API client")
public record CreateApiClientRequest(

        @Nullable
        @Size(max = 128)
        @Schema(description = "Optional custom clientId; if omitted, server generates one", example = "service-analytics")
        String clientId,

        @NotBlank
        @Size(max = 191)
        @Schema(description = "Display name/label", example = "Analytics Service")
        String name,

        @Schema(description = "Scopes (authorities) to grant; codes like 'client.read user.read'",
                example = "[\"client.read\",\"user.read\"]")
        List<@Size(max = 128) String> scopes,

        @Schema(description = "Allowed IPs/CIDRs (optional). Empty/null = no restriction",
                example = "[\"10.0.0.0/24\",\"203.0.113.10\"]")
        List<@Size(max = 64) String> allowedIps,

        @Schema(description = "Whether the client is enabled on creation", defaultValue = "true")
        Boolean enabled
) {}
