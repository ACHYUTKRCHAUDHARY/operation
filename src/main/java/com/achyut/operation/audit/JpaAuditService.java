package com.achyut.operation.audit;

import com.achyut.operation.common.AuditPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JpaAuditService implements AuditPort {
    private final AuditEventRepository repository;

    @Override
    public void record(String referenceType, Long referenceId, String action, String actor, String details) {
        repository.save(AuditEvent.builder()
            .referenceType(referenceType)
            .referenceId(referenceId)
            .action(action)
            .actor(actor == null || actor.isBlank() ? "system" : actor)
            .details(details)
            .build());
    }
}
