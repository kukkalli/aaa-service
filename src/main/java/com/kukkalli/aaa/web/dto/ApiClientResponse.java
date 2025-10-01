package com.kukkalli.aaa.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "ApiClientResponse", description = "Representation of an API client")
public record ApiClientResponse(

        Long id,

        @Schema(description = "Unique client identifier", example = "service-analytics")
        String clientId,

        @Schema(description = "Display name", example = "Analytics Service")
        String name,

        @Schema(description = "Scopes assigned to the client")
        List<String> scopes,

        @Schema(description = "Allowed IP addresses or CIDRs")
        List<String> allowedIps,

        @Schema(description = "Whether client is active", example = "true")
        boolean enabled,

        @Schema(description = "Creation timestamp (UTC ISO-8601)")
        Instant createdAt,

        @Schema(description = "Last update timestamp (UTC ISO-8601)")
        Instant updatedAt
) {}
