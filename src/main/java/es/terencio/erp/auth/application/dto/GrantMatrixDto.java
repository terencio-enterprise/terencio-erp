package es.terencio.erp.auth.application.dto;

import java.util.List;

public record GrantMatrixDto(
        Long grantId,
        String roleName,
        List<ModulePermissionsDto> modules) {
}
