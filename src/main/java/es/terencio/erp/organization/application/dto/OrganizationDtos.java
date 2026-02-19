package es.terencio.erp.organization.application.dto;

import java.util.List;
import java.util.UUID;

public final class OrganizationDtos {
    private OrganizationDtos() {}

    public record StoreTreeDto(UUID id, String name, String slug, String code, UUID companyId) {}
    public record CompanyTreeDto(UUID id, String name, String slug, UUID organizationId, List<StoreTreeDto> stores) {}
    public record OrganizationTreeDto(UUID id, String name, String slug, List<CompanyTreeDto> companies) {}
    public record DashboardContextDto(CompanyTreeDto activeCompany, StoreTreeDto activeStore, List<CompanyTreeDto> availableCompanies) {}
}
