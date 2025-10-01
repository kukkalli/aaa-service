package com.kukkalli.aaa.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/** Returned only once on create, includes the raw clientSecret. */
@Schema(name = "CreateApiClientResponse")
public record CreateApiClientResponse(
        Long id,
        @Schema(description = "Unique client identifier", example = "service-analytics")
        String clientId,
        @Schema(description = "Raw secret (SHOW ONLY ONCE!)", example = "cs_abcd...")
        String clientSecret,
        String name,
        List<String> scopes,
        List<String> allowedIps,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {}
