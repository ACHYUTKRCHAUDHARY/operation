package com.achyut.operation.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final IdempotencyRecordRepository repository;

    public record Reservation(IdempotencyRecord record, boolean acquired) {}

    public Optional<IdempotencyRecord> find(String key, String method, String path, String clientScope) {
        return repository.findByIdempotencyKeyAndHttpMethodAndRequestPathAndClientScope(key, method, path, clientScope)
            .filter(record -> record.getExpiresAt().isAfter(Instant.now()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Reservation reserve(String key, String method, String path, String clientScope,
                               String fingerprint, Duration ttl) {
        try {
            IdempotencyRecord record = repository.saveAndFlush(IdempotencyRecord.builder()
                .idempotencyKey(key)
                .httpMethod(method)
                .requestPath(path)
                .clientScope(clientScope)
                .requestFingerprint(fingerprint)
                .state(IdempotencyRecord.State.PROCESSING)
                .expiresAt(Instant.now().plus(ttl))
                .build());
            return new Reservation(record, true);
        } catch (DataIntegrityViolationException duplicate) {
            IdempotencyRecord existing = repository
                .findByIdempotencyKeyAndHttpMethodAndRequestPathAndClientScope(key, method, path, clientScope)
                .orElseThrow(() -> duplicate);
            return new Reservation(existing, false);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Long id, int status, String contentType, String body) {
        IdempotencyRecord record = repository.findById(id).orElseThrow();
        record.setState(IdempotencyRecord.State.COMPLETED);
        record.setResponseStatus(status);
        record.setResponseContentType(contentType);
        record.setResponseBody(body);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(Long id) {
        repository.deleteById(id);
    }
}
