package com.kukkalli.aaa.web.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Login request payload for user-to-system authentication.
 */
@Schema(name = "AuthRequest")
public record AuthRequest(

        @NotBlank
        @Schema(description = "Username or email of the user", example = "alice")
        String usernameOrEmail,

        @NotBlank
        @Schema(description = "Plaintext password", example = "P@ssw0rd!")
        String password
) {}
