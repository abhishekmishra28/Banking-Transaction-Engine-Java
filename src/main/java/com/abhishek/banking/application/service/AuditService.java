package com.abhishek.banking.application.service;

import com.abhishek.banking.domain.model.AuditLog;
import com.abhishek.banking.repository.interfaces.AuditLogRepository;

public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logEvent(String eventType, String description, String entityId) {
        AuditLog log = AuditLog.create(eventType, description, entityId);
        auditLogRepository.save(log);
    }
}
