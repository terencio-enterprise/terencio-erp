package es.terencio.erp.auth.application.dto;

public record PermissionNodeDto(
        String code,
        String name,
        boolean value,
        PermissionSource source) {
}
