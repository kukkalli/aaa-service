package com.kukkalli.aaa.web.mapper;

import com.kukkalli.aaa.domain.entity.Permission;
import com.kukkalli.aaa.domain.entity.Role;
import com.kukkalli.aaa.web.dto.RoleDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoleMapper {

    public RoleDto toDto(Role r) {
        List<String> permCodes = r.getPermissions().stream()
                .map(Permission::getCode)
                .sorted()
                .toList();

        return new RoleDto(
                r.getId(),
                r.getCode(),
                r.getName(),
                r.getDescription(),
                permCodes,
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
