package es.terencio.erp.auth.application.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import es.terencio.erp.organization.application.dto.CompanyTreeDto;

public record EmployeeInfoDto(
        Long id,
        String username,
        String fullName,
        boolean isActive,
        UUID lastCompanyId,
        UUID lastStoreId,
        List<CompanyTreeDto> companies,
        Map<String, Map<UUID, List<String>>> permissions) {
}
