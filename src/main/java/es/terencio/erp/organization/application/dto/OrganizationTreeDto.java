package es.terencio.erp.organization.application.dto;

import java.util.List;
import java.util.UUID;

public record OrganizationTreeDto(
        UUID id,
        String name,
        String slug,
        List<CompanyTreeDto> companies) {
}
