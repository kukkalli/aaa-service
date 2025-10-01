package com.kukkalli.aaa.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Response returned after successful authentication (login or refresh).
 */
@Schema(name = "AuthResponse")
public record AuthResponse(

        @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIs...")
        String accessToken,

        @Schema(description = "Refresh token (opaque, random string or UUID)")
        String refreshToken,

        @Schema(description = "Access token expiry time (UTC ISO-8601)")
        Instant accessTokenExpiresAt
) {}
