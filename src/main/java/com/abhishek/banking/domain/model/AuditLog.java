package com.abhishek.banking.domain.model;

import java.time.Instant;
import java.util.UUID;

public class AuditLog {
    private final String id;
    private final String eventType;
    private final String description;
    private final String entityId;
    private final Instant timestamp;

    public AuditLog(String id, String eventType, String description, String entityId, Instant timestamp) {
        this.id = id;
        this.eventType = eventType;
        this.description = description;
        this.entityId = entityId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }
    
    public static AuditLog create(String eventType, String description, String entityId) {
        return new AuditLog(UUID.randomUUID().toString(), eventType, description, entityId, Instant.now());
    }

    public String getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getDescription() {
        return description;
    }

    public String getEntityId() {
        return entityId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
