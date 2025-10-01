package com.kukkalli.aaa.web.mapper;

import com.kukkalli.aaa.domain.entity.Permission;
import com.kukkalli.aaa.domain.entity.Role;
import com.kukkalli.aaa.domain.entity.User;
import com.kukkalli.aaa.web.dto.UserResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {

    public UserResponse toResponse(User u) {
        List<String> roleCodes = u.getRoles().stream()
                .map(Role::getCode)
                .sorted()
                .toList();

        // Collect distinct permission codes across all roles
        List<String> permissions = u.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getCode)
                .distinct()
                .sorted()
                .toList();

        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.getPhone(),
                u.isEnabled(),
                u.isAccountNonLocked(),
                u.isAccountNonExpired(),
                u.isCredentialsNonExpired(),
                roleCodes,
                permissions,
                u.getCreatedAt(),
                u.getUpdatedAt()
        );
    }
}
