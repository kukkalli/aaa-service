package com.kukkalli.aaa.web.controller;

import com.kukkalli.aaa.service.ApiClientService;
import com.kukkalli.aaa.web.dto.ApiClientResponse;
import com.kukkalli.aaa.web.dto.CreateApiClientRequest;
import com.kukkalli.aaa.web.dto.CreateApiClientResponse;
import com.kukkalli.aaa.web.dto.UpdateApiClientRequest;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "API Clients", description = "Service-to-service client management")
public class ClientController {

    private final ApiClientService clientService;

    // ---------------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------------

    @GetMapping
    @PreAuthorize("hasAuthority('client.read')")
    @Operation(summary = "List API clients (paged)")
    public Page<ApiClientResponse> list(Pageable pageable,
                                        @RequestParam(required = false) String q,
                                        @RequestParam(required = false) Boolean enabled) {
        return clientService.search(q, enabled, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('client.read')")
    @Operation(summary = "Get API client by id")
    public ResponseEntity<ApiClientResponse> get(@PathVariable Long id) {
        return clientService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ---------------------------------------------------------------------
    // Write
    // ---------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasAuthority('client.create')")
    @Operation(summary = "Create a new API client (returns generated secret once)")
    public ResponseEntity<CreateApiClientResponse> create(@Valid @RequestBody CreateApiClientRequest req,
                                                          UriComponentsBuilder ucb) {
        CreateApiClientResponse created = clientService.create(req);
        URI location = ucb.path("/api/v1/clients/{id}").buildAndExpand(created.id()).toUri();

        return ResponseEntity.created(location)
                .header("X-Client-Secret-Once", "true")
                .header("Warning", "Client secret is returned only once. Store it securely.")
                .body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('client.update')")
    @Operation(summary = "Update an API client (name, scopes, allowedIps, enabled)")
    public ResponseEntity<ApiClientResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateApiClientRequest req) {
        ApiClientResponse updated = clientService.update(id, req);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('client.delete')")
    @Operation(summary = "Delete an API client")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------------
    // Secret ops
    // ---------------------------------------------------------------------

    @PostMapping("/{id}/rotate-secret")
    @PreAuthorize("hasAuthority('client.update')")
    @Operation(summary = "Rotate client secret (returns NEW secret once)")
    public ResponseEntity<Map<String, String>> rotateSecret(@PathVariable Long id) {
        String newSecret = clientService.rotateSecret(id);
        return ResponseEntity.ok()
                .header("X-Client-Secret-Once", "true")
                .header("Warning", "Client secret is returned only once. Store it securely.")
                .body(Map.of(
                        "clientId", clientService.getClientId(id),
                        "clientSecret", newSecret
                ));
    }
}
