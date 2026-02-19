package es.terencio.erp.auth.domain.model;

import java.util.Set;
import java.util.UUID;

public record AccessGrant(
        AccessScope scope,
        UUID targetId,
        String role,
        Set<String> extraPermissions,
        Set<String> excludedPermissions) {
}
