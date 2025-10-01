package com.kukkalli.aaa.service;

import com.kukkalli.aaa.domain.entity.ApiClient;
import com.kukkalli.aaa.domain.repository.ApiClientRepository;
import com.kukkalli.aaa.web.dto.ApiClientResponse;
import com.kukkalli.aaa.web.dto.CreateApiClientRequest;
import com.kukkalli.aaa.web.dto.CreateApiClientResponse;
import com.kukkalli.aaa.web.dto.UpdateApiClientRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApiClientService {

    private final ApiClientRepository repo;
    private final PasswordEncoder encoder;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiClientService(ApiClientRepository repo,
                            PasswordEncoder encoder,
                            AuditService auditService) {
        this.repo = Objects.requireNonNull(repo);
        this.encoder = Objects.requireNonNull(encoder);
        this.auditService = Objects.requireNonNull(auditService);
    }

    // ---------------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ApiClientResponse> search(String q, Boolean enabled, Pageable pageable) {
        Specification<ApiClient> spec = startSpec();

        if (enabled != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("enabled"), enabled));
        }

        if (StringUtils.hasText(q)) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("clientId")), like),
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("scopes")), like),
                    cb.like(cb.lower(root.get("allowedIps")), like)
            ));
        }

        return repo.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<ApiClientResponse> findById(Long id) {
        return repo.findById(id).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public String getClientId(Long id) {
        return repo.findById(id)
                .map(ApiClient::getClientId)
                .orElseThrow(() -> new NoSuchElementException("Client not found: " + id));
    }

    // ---------------------------------------------------------------------
    // Commands
    // ---------------------------------------------------------------------

    @Transactional
    public CreateApiClientResponse create(CreateApiClientRequest req) {
        String clientId = StringUtils.hasText(req.clientId())
                ? req.clientId().trim()
                : generateClientId();

        if (repo.existsByClientIdIgnoreCase(clientId)) {
            throw new IllegalArgumentException("clientId already exists: " + clientId);
        }

        // Generate one-time secret; store only the hash
        String rawSecret = generateClientSecret();
        String hash = encoder.encode(rawSecret);

        ApiClient entity = ApiClient.builder()
                .clientId(clientId)
                .clientSecretHash(hash)
                .name(req.name())
                .scopes(joinToCsv(req.scopes()))
                .allowedIps(joinToCsv(req.allowedIps()))
                .enabled(req.enabled() == null || req.enabled()) // default true when null
                .build();

        ApiClient saved = repo.save(entity);

        // Audit (do NOT log the secret)
        auditService.audit("CLIENT_CREATE", Map.of("id", saved.getId(), "clientId", saved.getClientId()));

        // Return metadata + the raw secret ONCE
        return new CreateApiClientResponse(
                saved.getId(),
                saved.getClientId(),
                rawSecret,
                saved.getName(),
                splitCsv(saved.getScopes()),
                splitCsv(saved.getAllowedIps()),
                saved.isEnabled(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    @Transactional
    public ApiClientResponse update(Long id, UpdateApiClientRequest req) {
        ApiClient entity = repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Client not found: " + id));

        boolean changed = false;

        if (StringUtils.hasText(req.name()) && !req.name().equals(entity.getName())) {
            entity.setName(req.name().trim());
            changed = true;
        }
        if (req.scopes() != null) {
            entity.setScopes(joinToCsv(req.scopes()));
            changed = true;
        }
        if (req.allowedIps() != null) {
            entity.setAllowedIps(joinToCsv(req.allowedIps()));
            changed = true;
        }
        if (req.enabled() != null && entity.isEnabled() != req.enabled()) {
            entity.setEnabled(req.enabled());
            changed = true;
        }

        if (changed) {
            entity = repo.save(entity);
            auditService.audit("CLIENT_UPDATE", Map.of("id", id));
        }

        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NoSuchElementException("Client not found: " + id);
        }
        repo.deleteById(id);
        auditService.audit("CLIENT_DELETE", Map.of("id", id));
    }

    /**
     * Rotates the client secret and returns the NEW raw secret (present it ONCE to the caller).
     */
    @Transactional
    public String rotateSecret(Long id) {
        ApiClient entity = repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Client not found: " + id));

        String raw = generateClientSecret();
        entity.setClientSecretHash(encoder.encode(raw));
        repo.save(entity);

        auditService.audit("CLIENT_ROTATE_SECRET", Map.of("id", id));

        return raw;
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** Start a no-op Specification (replacement for deprecated Specification.where(null)). */
    private static <T> Specification<T> startSpec() {
        // If your Spring Data JPA version supports it, you can use Specification.unrestricted() here.
        return (root, query, cb) -> null;
    }

    private ApiClientResponse toResponse(ApiClient ac) {
        return new ApiClientResponse(
                ac.getId(),
                ac.getClientId(),
                ac.getName(),
                splitCsv(ac.getScopes()),
                splitCsv(ac.getAllowedIps()),
                ac.isEnabled(),
                ac.getCreatedAt(),
                ac.getUpdatedAt()
        );
    }

    private String generateClientId() {
        // short, URL-safe suffix
        byte[] buf = new byte[6]; // 48 bits → ~8 chars base64url
        secureRandom.nextBytes(buf);
        String suffix = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        return "svc_" + suffix.toLowerCase(Locale.ROOT);
    }

    private String generateClientSecret() {
        byte[] buf = new byte[32]; // 256 bits
        secureRandom.nextBytes(buf);
        return "cs_" + Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String joinToCsv(List<String> values) {
        if (CollectionUtils.isEmpty(values)) return null;
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(","));
    }

    private static List<String> splitCsv(String csv) {
        if (!StringUtils.hasText(csv)) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
