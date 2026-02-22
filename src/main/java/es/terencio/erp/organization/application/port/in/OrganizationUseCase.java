package es.terencio.erp.organization.application.port.in;

import java.util.List;
import java.util.UUID;

import es.terencio.erp.organization.application.dto.OrganizationDtos.CompanyTreeDto;

public interface OrganizationUseCase {
    List<CompanyTreeDto> getTreeForEmployee(Long employeeId);
    void switchContext(Long employeeId, UUID companyId, UUID storeId);
}