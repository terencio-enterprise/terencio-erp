package es.terencio.erp.organization.application.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.auth.domain.model.AccessGrant;
import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.employees.application.port.out.EmployeePort;
import es.terencio.erp.organization.application.dto.CompanyTreeDto;
import es.terencio.erp.organization.application.dto.StoreTreeDto;
import es.terencio.erp.organization.application.port.out.OrganizationRepository;

@Service
@Transactional(readOnly = true)
public class OrganizationTreeService {

    private final EmployeePort employeePort;
    private final OrganizationRepository organizationRepository;

    public OrganizationTreeService(EmployeePort employeePort, OrganizationRepository organizationRepository) {
        this.employeePort = employeePort;
        this.organizationRepository = organizationRepository;
    }

    public List<CompanyTreeDto> getCompanyTreeForEmployee(Long employeeId) {
        Set<UUID> visibleOrganizationIds = new HashSet<>();
        Set<UUID> visibleCompanyIds = new HashSet<>();
        Set<UUID> visibleStoreIds = new HashSet<>();

        // 1. Fetch Grants
        List<AccessGrant> grants = employeePort.findAccessGrants(employeeId);

        for (AccessGrant grant : grants) {
            if (grant.scope() == AccessScope.ORGANIZATION) {
                visibleOrganizationIds.add(grant.targetId());
            } else if (grant.scope() == AccessScope.COMPANY) {
                visibleCompanyIds.add(grant.targetId());
            } else if (grant.scope() == AccessScope.STORE) {
                visibleStoreIds.add(grant.targetId());
            }
        }

        // 2. Expand Downwards
        if (!visibleOrganizationIds.isEmpty()) {
            List<CompanyTreeDto> companies = organizationRepository
                    .findCompaniesByOrganizationIds(visibleOrganizationIds);
            visibleCompanyIds.addAll(companies.stream().map(CompanyTreeDto::id).collect(Collectors.toSet()));
        }

        if (!visibleCompanyIds.isEmpty()) {
            List<StoreTreeDto> stores = organizationRepository.findStoresByCompanyIds(visibleCompanyIds);
            visibleStoreIds.addAll(stores.stream().map(StoreTreeDto::id).collect(Collectors.toSet()));
        }

        // 3. Expand Upwards
        if (!visibleStoreIds.isEmpty()) {
            visibleCompanyIds.addAll(organizationRepository.findCompanyIdsByStoreIds(visibleStoreIds));
        }

        // 4. Fetch All Entities
        List<CompanyTreeDto> companies = organizationRepository.findCompaniesByIds(visibleCompanyIds);
        List<StoreTreeDto> stores = organizationRepository.findStoresByIds(visibleStoreIds);

        // 5. Build Tree
        java.util.Map<UUID, List<StoreTreeDto>> storesByCompany = stores.stream()
                .filter(s -> s.companyId() != null)
                .collect(Collectors.groupingBy(StoreTreeDto::companyId));

        return companies.stream()
                .map(c -> new CompanyTreeDto(
                        c.id(),
                        c.name(),
                        c.slug(),
                        c.organizationId(),
                        storesByCompany.getOrDefault(c.id(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Transactional
    public void switchContext(Long employeeId, UUID companyId, UUID storeId) {
        List<CompanyTreeDto> companies = getCompanyTreeForEmployee(employeeId);

        boolean companyValid = companyId == null;
        boolean storeValid = storeId == null;

        if (companyId != null) {
            companyValid = companies.stream()
                    .anyMatch(c -> c.id().equals(companyId));
        }

        if (storeId != null) {
            storeValid = companies.stream()
                    .flatMap(c -> c.stores().stream())
                    .anyMatch(s -> s.id().equals(storeId));
        }

        if (!companyValid || !storeValid) {
            throw new IllegalArgumentException(
                    "Invalid context: User does not have access to specified company or store");
        }

        employeePort.updateLastActiveContext(employeeId, companyId, storeId);
    }
}
