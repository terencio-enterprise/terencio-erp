package es.terencio.erp.auth.application.dto;

import java.util.Set;

public record UpdateGrantRequest(
        String roleName,
        Set<String> extraPermissions,
        Set<String> excludedPermissions) {
}
