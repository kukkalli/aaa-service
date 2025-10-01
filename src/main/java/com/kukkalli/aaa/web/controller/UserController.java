package com.kukkalli.aaa.web.controller;

import com.kukkalli.aaa.domain.entity.User;
import com.kukkalli.aaa.service.UserService;
import com.kukkalli.aaa.web.dto.CreateUserRequest;
import com.kukkalli.aaa.web.dto.UpdateUserRequest;
import com.kukkalli.aaa.web.dto.UserResponse;
import com.kukkalli.aaa.web.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    // ---------------------------------------------------------------------
    // Read APIs
    // ---------------------------------------------------------------------

    @GetMapping
    @PreAuthorize("hasAuthority('user.read')")
    @Operation(summary = "List users (paged)")
    public Page<UserResponse> list(Pageable pageable,
                                   @RequestParam(required = false) String q) {
        Page<User> page = userService.search(q, pageable);
        return page.map(userMapper::toResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user.read')")
    @Operation(summary = "Get a user by id")
    public ResponseEntity<UserResponse> get(@PathVariable Long id) {
        return userService.findById(id)
                .map(userMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ---------------------------------------------------------------------
    // Write APIs
    // ---------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasAuthority('user.create')")
    @Operation(summary = "Create a new user")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req,
                                               UriComponentsBuilder ucb) {
        User created = userService.create(req);
        URI location = ucb.path("/api/v1/users/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(userMapper.toResponse(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user.update')")
    @Operation(summary = "Update an existing user")
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateUserRequest req) {
        User updated = userService.update(id, req);
        return ResponseEntity.ok(userMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user.delete')")
    @Operation(summary = "Delete (or disable) a user")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @RequestParam(defaultValue = "true") boolean soft) {
        if (soft) {
            userService.disable(id);
        } else {
            userService.delete(id);
        }
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------------
    // Role management
    // ---------------------------------------------------------------------

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('role.update')")
    @Operation(summary = "Assign roles to a user")
    public ResponseEntity<UserResponse> assignRoles(@PathVariable Long id,
                                                    @RequestBody List<String> roleCodes) {
        Assert.notEmpty(roleCodes, "roleCodes must not be empty");
        User updated = userService.assignRoles(id, roleCodes);
        return ResponseEntity.ok(userMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}/roles/{roleCode}")
    @PreAuthorize("hasAuthority('role.update')")
    @Operation(summary = "Remove a role from a user")
    public ResponseEntity<UserResponse> removeRole(@PathVariable Long id,
                                                   @PathVariable String roleCode) {
        User updated = userService.removeRole(id, roleCode);
        return ResponseEntity.ok(userMapper.toResponse(updated));
    }
}
