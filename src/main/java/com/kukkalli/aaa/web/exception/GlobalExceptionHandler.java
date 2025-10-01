package com.kukkalli.aaa.web.exception;

import com.kukkalli.aaa.service.AuditService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AuditService auditService;

    // 400 — validation errors
    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class,
            MissingServletRequestParameterException.class, IllegalArgumentException.class })
    public ResponseEntity<Object> handleBadRequest(Exception ex, HttpServletRequest req) {
        auditService.audit("HTTP_400_BAD_REQUEST", req, Map.of("path", req.getRequestURI()));
        return build(HttpStatus.BAD_REQUEST, "bad_request", ex.getMessage(), req);
    }

    // 401 — bad credentials
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleUnauthorized(BadCredentialsException ex, HttpServletRequest req) {
        auditService.audit("HTTP_401_UNAUTHORIZED", req, Map.of("path", req.getRequestURI()));
        return build(HttpStatus.UNAUTHORIZED, "unauthorized", "Invalid credentials", req);
    }

    // 403 — access denied
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleForbidden(AccessDeniedException ex, HttpServletRequest req) {
        auditService.audit("HTTP_403_FORBIDDEN", req, Map.of("path", req.getRequestURI()));
        return build(HttpStatus.FORBIDDEN, "forbidden", "Access is denied", req);
    }

    // 404 — entity not found
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(EntityNotFoundException ex, HttpServletRequest req) {
        auditService.audit("HTTP_404_NOT_FOUND", req, Map.of("path", req.getRequestURI()));
        return build(HttpStatus.NOT_FOUND, "not_found", ex.getMessage(), req);
    }

    // 409 — conflict or business rule violation
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleConflict(IllegalStateException ex, HttpServletRequest req) {
        auditService.audit("HTTP_409_CONFLICT", req, Map.of("path", req.getRequestURI()));
        return build(HttpStatus.CONFLICT, "conflict", ex.getMessage(), req);
    }

    // 500 — everything else
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneric(Exception ex, HttpServletRequest req) {
        auditService.audit("HTTP_500_ERROR", req, Map.of("path", req.getRequestURI()));
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "An unexpected error occurred",
                req);
    }

    private ResponseEntity<Object> build(HttpStatus status, String code, String message, HttpServletRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", code);
        body.put("message", message);
        body.put("path", req.getRequestURI());
        return ResponseEntity.status(status)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(body);
    }
}
