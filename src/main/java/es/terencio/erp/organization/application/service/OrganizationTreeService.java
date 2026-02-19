package es.terencio.erp.organization.application.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import es.terencio.erp.auth.domain.model.AccessGrant;
import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.employees.application.port.out.EmployeePort;
import es.terencio.erp.organization.application.dto.OrganizationDtos.CompanyTreeDto;
import es.terencio.erp.organization.application.dto.OrganizationDtos.StoreTreeDto;
import es.terencio.erp.organization.application.port.in.OrganizationTreeUseCase;
import es.terencio.erp.organization.application.port.out.OrganizationRepository;

public class OrganizationTreeService implements OrganizationTreeUseCase {
    private final EmployeePort employeePort;
    private final OrganizationRepository organizationRepository;

    public OrganizationTreeService(EmployeePort employeePort, OrganizationRepository organizationRepository) {
        this.employeePort = employeePort;
        this.organizationRepository = organizationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyTreeDto> getCompanyTreeForEmployee(Long employeeId) {
        Set<UUID> visibleOrganizationIds = new HashSet<>();
        Set<UUID> visibleCompanyIds = new HashSet<>();
        Set<UUID> visibleStoreIds = new HashSet<>();

        List<AccessGrant> grants = employeePort.findAccessGrants(employeeId);
        for (AccessGrant grant : grants) {
            if (grant.scope() == AccessScope.ORGANIZATION) visibleOrganizationIds.add(grant.targetId());
            else if (grant.scope() == AccessScope.COMPANY) visibleCompanyIds.add(grant.targetId());
            else if (grant.scope() == AccessScope.STORE) visibleStoreIds.add(grant.targetId());
        }

        if (!visibleOrganizationIds.isEmpty()) {
            List<CompanyTreeDto> companies = organizationRepository.findCompaniesByOrganizationIds(visibleOrganizationIds);
            visibleCompanyIds.addAll(companies.stream().map(CompanyTreeDto::id).collect(Collectors.toSet()));
        }
        if (!visibleCompanyIds.isEmpty()) {
            List<StoreTreeDto> stores = organizationRepository.findStoresByCompanyIds(visibleCompanyIds);
            visibleStoreIds.addAll(stores.stream().map(StoreTreeDto::id).collect(Collectors.toSet()));
        }
        if (!visibleStoreIds.isEmpty()) {
            visibleCompanyIds.addAll(organizationRepository.findCompanyIdsByStoreIds(visibleStoreIds));
        }

        List<CompanyTreeDto> companies = organizationRepository.findCompaniesByIds(visibleCompanyIds);
        List<StoreTreeDto> stores = organizationRepository.findStoresByIds(visibleStoreIds);

        Map<UUID, List<StoreTreeDto>> storesByCompany = stores.stream()
                .filter(s -> s.companyId() != null).collect(Collectors.groupingBy(StoreTreeDto::companyId));

        return companies.stream()
                .map(c -> new CompanyTreeDto(c.id(), c.name(), c.slug(), c.organizationId(), storesByCompany.getOrDefault(c.id(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void switchContext(Long employeeId, UUID companyId, UUID storeId) {
        List<CompanyTreeDto> companies = getCompanyTreeForEmployee(employeeId);

        boolean companyValid = companyId == null;
        boolean storeValid = storeId == null;

        if (companyId != null) companyValid = companies.stream().anyMatch(c -> c.id().equals(companyId));
        if (storeId != null) storeValid = companies.stream().flatMap(c -> c.stores().stream()).anyMatch(s -> s.id().equals(storeId));

        if (!companyValid || !storeValid) {
            throw new IllegalArgumentException("Invalid context: User does not have access to specified company or store");
        }
        employeePort.updateLastActiveContext(employeeId, companyId, storeId);
    }
}
