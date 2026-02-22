package es.terencio.erp.organization.application.port.out;

import java.util.List;

import es.terencio.erp.organization.application.dto.OrganizationDtos.CompanyTreeDto;

public interface OrganizationRepository {
    List<CompanyTreeDto> findFullTreeByEmployeeId(Long employeeId);
}