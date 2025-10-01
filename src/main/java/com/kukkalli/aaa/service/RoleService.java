package com.kukkalli.aaa.service;

import com.kukkalli.aaa.audit.Audited;
import com.kukkalli.aaa.domain.entity.Permission;
import com.kukkalli.aaa.domain.entity.Role;
import com.kukkalli.aaa.domain.repository.PermissionRepository;
import com.kukkalli.aaa.domain.repository.RoleRepository;
import com.kukkalli.aaa.web.dto.CreateRoleRequest;
import com.kukkalli.aaa.web.dto.UpdateRoleRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class RoleService {

    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;

    public RoleService(RoleRepository roleRepo, PermissionRepository permRepo) {
        this.roleRepo = Objects.requireNonNull(roleRepo);
        this.permRepo = Objects.requireNonNull(permRepo);
    }

    // ---------------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<Role> list(Pageable pageable) {
        return roleRepo.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Role> findById(Long id) {
        return roleRepo.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Role> findByCode(String code) {
        return roleRepo.findByCodeIgnoreCase(code);
    }

    // ---------------------------------------------------------------------
    // Commands
    // ---------------------------------------------------------------------

    @Audited(action = "ROLE_CREATE", targetType = "ROLE")
    @Transactional
    public Role createRole(CreateRoleRequest req) {
        if (!StringUtils.hasText(req.code())) {
            throw new IllegalArgumentException("Role code is required");
        }
        String code = req.code().trim();
        if (roleRepo.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Role code already exists: " + code);
        }

        Role role = Role.builder()
                .code(code)
                .name(req.name())
                .description(req.description())
                .permissions(new LinkedHashSet<>())
                .build();

        if (!CollectionUtils.isEmpty(req.permissions())) {
            role.getPermissions().addAll(resolvePermissions(req.permissions()));
        }

        return roleRepo.save(role);
    }

    @Audited(action = "ROLE_UPDATE", targetType = "ROLE", targetId = "#result.id")
    @Transactional
    public Role updateRole(Long id, UpdateRoleRequest req) {
        Role role = roleRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + id));

        boolean changed = false;

        if (StringUtils.hasText(req.name()) && !Objects.equals(req.name(), role.getName())) {
            role.setName(req.name());
            changed = true;
        }
        if (req.description() != null && !Objects.equals(req.description(), role.getDescription())) {
            role.setDescription(req.description());
            changed = true;
        }
        if (req.permissions() != null) {
            var newPerms = resolvePermissions(req.permissions());
            role.getPermissions().clear();
            role.getPermissions().addAll(newPerms);
            changed = true;
        }

        return changed ? roleRepo.save(role) : role;
    }

    @Audited(action = "ROLE_DELETE", targetType = "ROLE")
    @Transactional
    public void deleteRole(Long id) {
        if (!roleRepo.existsById(id)) {
            throw new EntityNotFoundException("Role not found: " + id);
        }
        roleRepo.deleteById(id);
    }

    @Audited(action = "PERMISSION_ASSIGN", targetType = "ROLE", targetId = "#roleCode")
    @Transactional
    public void assignPermission(String roleCode, String permCode) {
        Role role = roleRepo.findByCodeIgnoreCase(Objects.requireNonNull(roleCode))
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleCode));
        Permission perm = permRepo.findByCodeIgnoreCase(Objects.requireNonNull(permCode))
                .orElseThrow(() -> new EntityNotFoundException("Permission not found: " + permCode));

        if (role.getPermissions().add(perm)) {
            roleRepo.save(role);
        }
    }

    @Audited(action = "PERMISSION_REMOVE", targetType = "ROLE", targetId = "#roleCode")
    @Transactional
    public void removePermission(String roleCode, String permCode) {
        Role role = roleRepo.findByCodeIgnoreCase(Objects.requireNonNull(roleCode))
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleCode));
        Permission perm = permRepo.findByCodeIgnoreCase(Objects.requireNonNull(permCode))
                .orElseThrow(() -> new EntityNotFoundException("Permission not found: " + permCode));

        if (role.getPermissions().remove(perm)) {
            roleRepo.save(role);
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Set<Permission> resolvePermissions(List<String> codes) {
        Set<Permission> perms = new LinkedHashSet<>();
        for (String code : codes) {
            String c = code == null ? null : code.trim();
            if (!StringUtils.hasText(c)) continue;
            Permission p = permRepo.findByCodeIgnoreCase(c)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown permission: " + c));
            perms.add(p);
        }
        return perms;
    }
}

