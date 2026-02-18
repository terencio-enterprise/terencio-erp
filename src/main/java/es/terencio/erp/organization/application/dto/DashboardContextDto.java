package es.terencio.erp.organization.application.dto;

import java.util.List;

public record DashboardContextDto(
        CompanyTreeDto activeCompany,
        StoreTreeDto activeStore,
        List<CompanyTreeDto> availableCompanies) {
}
