package es.terencio.erp.organization.application.dto;

import java.util.List;
import java.util.UUID;

public record CompanyTreeDto(
        UUID id,
        String name,
        String slug,
        UUID organizationId,
        List<StoreTreeDto> stores) {
}
