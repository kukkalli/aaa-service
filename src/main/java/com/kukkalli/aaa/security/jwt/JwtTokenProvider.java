package com.kukkalli.aaa.security.jwt;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JWT provider using HMAC-SHA256 (HS256).
 * - Reads settings from application.yml: security.jwt.*
 * - Encodes authorities as a space-delimited "scope" claim.
 * - Uses issuer + jti to help with audit/tracing.
 */
@Component
public class JwtTokenProvider {

    private final String issuer;
    private final byte[] secretBytes;
    private final Duration accessTtl;
    private final Duration refreshTtl; // kept for symmetry (if you decide to use JWT refresh later)
    private final Clock clock;

    public JwtTokenProvider(
            @Value("${security.jwt.issuer:aaa-service}") String issuer,
            @Value("${security.jwt.secret:please-change-in-prod}") String secret,
            @Value("${security.jwt.access-token.ttl:PT15M}") Duration accessTtl,
            @Value("${security.jwt.refresh-token.ttl:P7D}") Duration refreshTtl,
            Clock clock
    ) {
        this.issuer = Objects.requireNonNull(issuer);
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
        this.clock = clock;
    }

    // ---------- Create Access Token ------------------------------------

    public String generateAccessToken(UserDetails principal) {
        String username = principal.getUsername();
        String scope = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .collect(Collectors.joining(" "));

        Instant now = clock.instant();
        Instant exp = now.plus(accessTtl);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(username)
                .issuer(issuer)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", scope)  // space-delimited authorities (roles + permissions)
                .build();

        return sign(claims);
    }

    // ---------- Parse / Validate ---------------------------------------

    public boolean validate(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (verify(jwt)) {
                Date exp = jwt.getJWTClaimsSet().getExpirationTime();
                return exp != null && exp.toInstant().isAfter(clock.instant());
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }


    public Optional<JWTClaimsSet> parseClaims(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!verify(jwt))
                return Optional.empty();
            return Optional.of(jwt.getJWTClaimsSet());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<String> extractUsername(String token) {
        return parseClaims(token).map(JWTClaimsSet::getSubject);
    }

    public List<String> extractAuthorities(String token) {
        return parseClaims(token)
                .map(c -> {
                    Object raw = c.getClaim("scope");
                    if (raw == null) return List.<String>of();
                    return Arrays.stream(raw.toString().split("\\s+"))
                            .filter(s -> !s.isBlank())
                            .map(String::valueOf)   // ensure Stream<String>
                            .toList();
                })
                .orElse(List.of());
    }

    // ---------- Internals ----------------------------------------------

    private String sign(JWTClaimsSet claims) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                    .type(JOSEObjectType.JWT)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claims);
            JWSSigner signer = new MACSigner(secretBytes);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    private boolean verify(SignedJWT jwt) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(secretBytes);
        return jwt.verify(verifier)
                && issuer.equals(jwt.getJWTClaimsSet().getIssuer());
    }
}
