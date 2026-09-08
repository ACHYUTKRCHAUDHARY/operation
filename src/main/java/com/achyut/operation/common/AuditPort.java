package com.achyut.operation.common;

public interface AuditPort {
    void record(String referenceType, Long referenceId, String action, String actor, String details);
}
