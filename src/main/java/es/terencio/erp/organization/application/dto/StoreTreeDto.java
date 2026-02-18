package es.terencio.erp.organization.application.dto;

import java.util.UUID;

public record StoreTreeDto(
        UUID id,
        String name,
        String slug,
        String code,
        UUID companyId) {
}
