package com.achyut.operation.idempotency;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "idempotency_records",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_idempotency_scope",
        columnNames = {"idempotency_key", "http_method", "request_path", "client_scope"}
    ),
    indexes = @Index(name = "idx_idempotency_expires_at", columnList = "expires_at")
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IdempotencyRecord {
    public enum State { PROCESSING, COMPLETED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "request_path", nullable = false, length = 500)
    private String requestPath;

    @Column(name = "client_scope", nullable = false, length = 64)
    private String clientScope;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private State state;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_content_type", length = 200)
    private String responseContentType;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (state == null) state = State.PROCESSING;
    }
}
