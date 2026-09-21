package com.abhishek.banking.repository.interfaces;

import com.abhishek.banking.domain.model.AuditLog;

public interface AuditLogRepository {
    void save(AuditLog log);
}
