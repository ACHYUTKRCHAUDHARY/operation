package com.achyut.operation.audit;

import com.achyut.operation.common.AuditPort;
import com.achyut.operation.service.usecase.AuditQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class JpaAuditService implements AuditPort, AuditQuery {
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

    @Override
    public List<AuditEvent> timeline(String type, Long id) {
        return repository.findTop100ByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(type.toUpperCase(Locale.ROOT), id);
    }
}
