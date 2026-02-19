package es.terencio.erp.auth.domain.model;

import java.util.UUID;

public record PermissionContext(
        AccessScope scope,
        UUID targetId,
        String permission) {
}
