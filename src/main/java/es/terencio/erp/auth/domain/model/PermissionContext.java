package es.terencio.erp.auth.domain.model;

import java.util.UUID;

/**
 * Represents a specific permission granted within a specific context.
 * Flattened tuple: (Scope, TargetId, PermissionCode)
 */
public record PermissionContext(
        AccessScope scope,
        UUID targetId,
        String permission) {
}
