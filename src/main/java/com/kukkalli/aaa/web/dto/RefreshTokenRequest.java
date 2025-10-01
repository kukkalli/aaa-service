package com.kukkalli.aaa.web.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request payload for refreshing an access token using a refresh token.
 */
@Schema(name = "RefreshTokenRequest")
public record RefreshTokenRequest(

        @NotBlank
        @Schema(description = "Opaque refresh token string", example = "8d3b0d6f-12c4-4d31-b21e-09f3e28fbb77")
        String refreshToken
) {}
