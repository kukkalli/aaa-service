package com.kukkalli.aaa.web.controller;

import com.kukkalli.aaa.domain.entity.Permission;
import com.kukkalli.aaa.service.PermissionService;
import com.kukkalli.aaa.web.dto.CreatePermissionRequest;
import com.kukkalli.aaa.web.dto.PermissionDto;
import com.kukkalli.aaa.web.dto.UpdatePermissionRequest;
import com.kukkalli.aaa.web.mapper.PermissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Tag(name = "Permissions", description = "Permission catalog & management")
public class PermissionsController {

    private final PermissionService permissionService;
    private final PermissionMapper permissionMapper;

    // ---- Read ----

    @GetMapping
    @PreAuthorize("hasAuthority('permission.read')")
    @Operation(summary = "List permissions (paged)")
    public Page<PermissionDto> list(Pageable pageable) {
        Page<Permission> page = permissionService.list(pageable);
        return page.map(permissionMapper::toDto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('permission.read')")
    @Operation(summary = "Get permission by id")
    public ResponseEntity<PermissionDto> get(@PathVariable Long id) {
        return permissionService.findById(id)
                .map(permissionMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('permission.read')")
    @Operation(summary = "Get permission by code")
    public ResponseEntity<PermissionDto> getByCode(@PathVariable String code) {
        return permissionService.findByCode(code)
                .map(permissionMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ---- Write (audited in service) ----

    @PostMapping
    @PreAuthorize("hasAuthority('permission.create')")
    @Operation(summary = "Create a new permission")
    public ResponseEntity<PermissionDto> create(@Valid @RequestBody CreatePermissionRequest req,
                                                UriComponentsBuilder ucb) {
        Permission created = permissionService.create(req);
        URI location = ucb.path("/api/v1/permissions/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(permissionMapper.toDto(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('permission.update')")
    @Operation(summary = "Update an existing permission")
    public ResponseEntity<PermissionDto> update(@PathVariable Long id,
                                                @Valid @RequestBody UpdatePermissionRequest req) {
        Permission updated = permissionService.update(id, req);
        return ResponseEntity.ok(permissionMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('permission.delete')")
    @Operation(summary = "Delete a permission")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
