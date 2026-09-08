package com.achyut.operation.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {
    Optional<IdempotencyRecord> findByIdempotencyKeyAndHttpMethodAndRequestPathAndClientScope(
        String idempotencyKey,
        String httpMethod,
        String requestPath,
        String clientScope
    );

    long deleteByExpiresAtBefore(Instant cutoff);
}
