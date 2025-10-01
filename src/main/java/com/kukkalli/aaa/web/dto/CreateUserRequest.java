package com.kukkalli.aaa.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(name = "CreateUserRequest")
public record CreateUserRequest(
        @NotBlank @Size(max = 64)
        @Schema(example = "alice")
        String username,

        @NotBlank @Email @Size(max = 191)
        @Schema(example = "alice@example.com")
        String email,

        @NotBlank @Size(min = 8, max = 128)
        @Schema(description = "Plaintext password", example = "P@ssw0rd!")
        String password,

        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 40)  String phone,

        @Schema(description = "Role codes to assign (e.g., ROLE_USER, ROLE_ADMIN)")
        List<String> roles
) {}
