package com.achyut.operation.service.usecase;

import com.achyut.operation.audit.AuditEvent;
import java.util.List;

public interface AuditQuery {
    List<AuditEvent> timeline(String type, Long id);
}
