package es.terencio.erp.organization.application.dto;

import java.util.List;

public record DashboardContextDto(
        CompanyTreeDto activeCompany,
        StoreTreeDto activeStore,
        List<OrganizationTreeDto> availableOrganizations,
        List<CompanyTreeDto> availableCompanies, // Flattened list of available companies for easier selection if needed
        List<StoreTreeDto> availableStores // Flattened list of visible stores
) {
    // We can also just return the tree in availableOrganizations and let frontend
    // parse.
    // But for "active context", we specifically want the details of the active
    // ones.
}
