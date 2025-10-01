package com.kukkalli.aaa.web.controller;

import com.kukkalli.aaa.domain.entity.Role;
import com.kukkalli.aaa.service.RoleService;
import com.kukkalli.aaa.web.dto.CreateRoleRequest;
import com.kukkalli.aaa.web.dto.RoleDto;
import com.kukkalli.aaa.web.dto.UpdateRoleRequest;
import com.kukkalli.aaa.web.mapper.RoleMapper;
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
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Role & permission management")
public class RoleController {

    private final RoleService roleService;
    private final RoleMapper roleMapper;

    // ---------------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------------

    @GetMapping
    @PreAuthorize("hasAuthority('role.read')")
    @Operation(summary = "List roles (paged)")
    public Page<RoleDto> list(Pageable pageable) {
        return roleService.list(pageable).map(roleMapper::toDto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role.read')")
    @Operation(summary = "Get role by id")
    public ResponseEntity<RoleDto> get(@PathVariable Long id) {
        return roleService.findById(id)
                .map(roleMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('role.read')")
    @Operation(summary = "Get role by code")
    public ResponseEntity<RoleDto> getByCode(@PathVariable String code) {
        return roleService.findByCode(code)
                .map(roleMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ---------------------------------------------------------------------
    // Write
    // ---------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasAuthority('role.create')")
    @Operation(summary = "Create a new role")
    public ResponseEntity<RoleDto> create(@Valid @RequestBody CreateRoleRequest req,
                                          UriComponentsBuilder ucb) {
        Role created = roleService.createRole(req);
        URI location = ucb.path("/api/v1/roles/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(roleMapper.toDto(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role.update')")
    @Operation(summary = "Update an existing role")
    public ResponseEntity<RoleDto> update(@PathVariable Long id,
                                          @Valid @RequestBody UpdateRoleRequest req) {
        Role updated = roleService.updateRole(id, req);
        return ResponseEntity.ok(roleMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role.delete')")
    @Operation(summary = "Delete a role")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------------
    // Permission assignments
    // ---------------------------------------------------------------------

    @PostMapping("/{roleCode}/permissions/{permCode}")
    @PreAuthorize("hasAuthority('role.update')")
    @Operation(summary = "Assign a permission to a role")
    public ResponseEntity<RoleDto> assignPermission(@PathVariable String roleCode,
                                                    @PathVariable String permCode) {
        roleService.assignPermission(roleCode, permCode);
        return roleService.findByCode(roleCode)
                .map(roleMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{roleCode}/permissions/{permCode}")
    @PreAuthorize("hasAuthority('role.update')")
    @Operation(summary = "Remove a permission from a role")
    public ResponseEntity<RoleDto> removePermission(@PathVariable String roleCode,
                                                    @PathVariable String permCode) {
        roleService.removePermission(roleCode, permCode);
        return roleService.findByCode(roleCode)
                .map(roleMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
