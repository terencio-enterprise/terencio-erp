package es.terencio.erp.audit.domain.model;

import java.time.Instant;
import java.util.Map;

import es.terencio.erp.shared.domain.identifier.UserId;

/**
 * AuditUserAction entity.
 * Records user actions for security audit trail.
 */
public class AuditUserAction {

    private final Long id;
    private final UserId userId;
    private final String action;
    private final String entity;
    private final String entityId;
    private final Map<String, Object> payload;
    private final Instant createdAt;

    public AuditUserAction(
            Long id,
            UserId userId,
            String action,
            String entity,
            String entityId,
            Map<String, Object> payload,
            Instant createdAt) {

        this.id = id;
        this.userId = userId;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.payload = payload;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static AuditUserAction log(UserId userId, String action, String entity, String entityId) {
        return new AuditUserAction(null, userId, action, entity, entityId, null, Instant.now());
    }

    // Getters
    public Long id() {
        return id;
    }

    public UserId userId() {
        return userId;
    }

    public String action() {
        return action;
    }

    public String entity() {
        return entity;
    }

    public String entityId() {
        return entityId;
    }

    public Map<String, Object> payload() {
        return payload;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
