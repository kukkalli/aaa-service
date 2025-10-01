package com.kukkalli.aaa.web.mapper;

import com.kukkalli.aaa.domain.entity.Permission;
import com.kukkalli.aaa.web.dto.PermissionDto;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {

    public PermissionDto toDto(Permission p) {
        return new PermissionDto(
                p.getId(),
                p.getCode(),
                p.getName(),
                p.getDescription(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
