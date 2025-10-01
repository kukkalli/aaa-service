package com.kukkalli.aaa.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "UserResponse")
public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        boolean enabled,
        boolean accountNonLocked,
        boolean accountNonExpired,
        boolean credentialsNonExpired,
        List<String> roles,        // role codes
        List<String> permissions,  // permission codes (deduped)
        Instant createdAt,
        Instant updatedAt
) {}
