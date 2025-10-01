package com.kukkalli.aaa.service;

import com.kukkalli.aaa.audit.Audited;
import com.kukkalli.aaa.domain.entity.Permission;
import com.kukkalli.aaa.domain.repository.PermissionRepository;
import com.kukkalli.aaa.web.dto.CreatePermissionRequest;
import com.kukkalli.aaa.web.dto.UpdatePermissionRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

@Service
public class PermissionService {

    private final PermissionRepository repo;

    public PermissionService(PermissionRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    // ---- Queries ----
    @Transactional(readOnly = true)
    public Page<Permission> list(Pageable pageable) { return repo.findAll(pageable); }

    @Transactional(readOnly = true)
    public Optional<Permission> findById(Long id) { return repo.findById(id); }

    @Transactional(readOnly = true)
    public Optional<Permission> findByCode(String code) { return repo.findByCodeIgnoreCase(code); }

    @Transactional(readOnly = true)
    public Permission getByCodeOrThrow(String code) {
        return repo.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found: " + code));
    }

    // ---- Commands (audited) ----
    @Audited(action = "PERMISSION_CREATE", targetType = "PERMISSION", targetId = "#req.code")
    @Transactional
    public Permission create(CreatePermissionRequest req) {
        if (!StringUtils.hasText(req.code())) throw new IllegalArgumentException("Code is required");
        String code = req.code().trim();
        if (repo.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Permission code already exists: " + code);
        }
        Permission p = Permission.builder()
                .code(code)
                .name(req.name())
                .description(req.description())
                .build();
        return repo.save(p);
    }

    @Audited(action = "PERMISSION_UPDATE", targetType = "PERMISSION", targetId = "#id")
    @Transactional
    public Permission update(Long id, UpdatePermissionRequest req) {
        Permission p = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found: " + id));

        boolean changed = false;
        if (StringUtils.hasText(req.name()) && !Objects.equals(req.name(), p.getName())) {
            p.setName(req.name());
            changed = true;
        }
        if (req.description() != null && !Objects.equals(req.description(), p.getDescription())) {
            p.setDescription(req.description());
            changed = true;
        }
        return changed ? repo.save(p) : p;
    }

    @Audited(action = "PERMISSION_DELETE", targetType = "PERMISSION", targetId = "#id")
    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new EntityNotFoundException("Permission not found: " + id);
        repo.deleteById(id);
    }
}
