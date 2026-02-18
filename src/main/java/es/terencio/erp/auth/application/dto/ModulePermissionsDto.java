package es.terencio.erp.auth.application.dto;

import java.util.List;

public record ModulePermissionsDto(
        String moduleName,
        List<PermissionNodeDto> permissions) {
}
