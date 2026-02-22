package es.terencio.erp.organization.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.employees.application.port.out.EmployeePort;
import es.terencio.erp.organization.application.dto.OrganizationDtos.CompanyTreeDto;
import es.terencio.erp.organization.application.port.in.OrganizationUseCase;
import es.terencio.erp.organization.application.port.out.OrganizationRepository;

public class OrganizationService implements OrganizationUseCase {

    private final EmployeePort employeePort;
    private final OrganizationRepository organizationRepository;

    public OrganizationService(EmployeePort employeePort, OrganizationRepository organizationRepository) {
        this.employeePort = employeePort;
        this.organizationRepository = organizationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyTreeDto> getTreeForEmployee(Long employeeId) {
        return organizationRepository.findFullTreeByEmployeeId(employeeId);
    }

    @Override
    @Transactional
    public void switchContext(Long employeeId, UUID companyId, UUID storeId) {
        // 1. Fetch available tree
        List<CompanyTreeDto> tree = getTreeForEmployee(employeeId);

        // 2. Validate Access
        boolean companyValid = companyId == null;
        boolean storeValid = storeId == null;

        if (companyId != null) {
            companyValid = tree.stream().anyMatch(c -> c.id().equals(companyId));
            if (companyValid && storeId != null) {
                 storeValid = tree.stream()
                        .filter(c -> c.id().equals(companyId))
                        .flatMap(c -> c.stores().stream())
                        .anyMatch(s -> s.id().equals(storeId));
            }
        }

        if (!companyValid || !storeValid) {
            throw new IllegalArgumentException("Invalid context switch: Access denied.");
        }

        // 3. Update Employee Context
        employeePort.updateLastActiveContext(employeeId, companyId, storeId);
    }
}