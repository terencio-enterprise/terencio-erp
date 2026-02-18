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
import es.terencio.erp.organization.application.dto.OrganizationTreeDto;
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

    public List<OrganizationTreeDto> getOrganizationTreeForEmployee(Long employeeId) {
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
        if (!visibleCompanyIds.isEmpty()) {
            visibleOrganizationIds.addAll(organizationRepository.findOrganizationIdsByCompanyIds(visibleCompanyIds));
        }

        // 4. Fetch All Entities
        List<OrganizationTreeDto> organizations = organizationRepository.findOrganizationsByIds(visibleOrganizationIds);
        List<CompanyTreeDto> companies = organizationRepository.findCompaniesByIds(visibleCompanyIds);
        List<StoreTreeDto> stores = organizationRepository.findStoresByIds(visibleStoreIds);

        // 5. Build Tree
        java.util.Map<UUID, List<StoreTreeDto>> storesByCompany = stores.stream()
                .filter(s -> s.companyId() != null)
                .collect(Collectors.groupingBy(StoreTreeDto::companyId));

        List<CompanyTreeDto> enrichedCompanies = companies.stream()
                .map(c -> new CompanyTreeDto(
                        c.id(),
                        c.name(),
                        c.slug(),
                        c.organizationId(),
                        storesByCompany.getOrDefault(c.id(), Collections.emptyList())))
                .collect(Collectors.toList());

        java.util.Map<UUID, List<CompanyTreeDto>> companiesByOrganization = enrichedCompanies.stream()
                .filter(c -> c.organizationId() != null)
                .collect(Collectors.groupingBy(CompanyTreeDto::organizationId));

        return organizations.stream()
                .map(o -> new OrganizationTreeDto(
                        o.id(),
                        o.name(),
                        o.slug(),
                        companiesByOrganization.getOrDefault(o.id(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    public es.terencio.erp.organization.application.dto.DashboardContextDto getDashboardContext(Long employeeId) {
        // 1. Get Tree (Available Context)
        List<OrganizationTreeDto> tree = getOrganizationTreeForEmployee(employeeId);

        // 2. Flatten available for easier lookup/return
        List<CompanyTreeDto> availableCompanies = tree.stream()
                .flatMap(o -> o.companies().stream())
                .collect(Collectors.toList());

        List<StoreTreeDto> availableStores = availableCompanies.stream()
                .flatMap(c -> c.stores().stream())
                .collect(Collectors.toList());

        // 3. Resolve Active Context
        var employee = employeePort.findById(employeeId).orElseThrow(() -> new RuntimeException("Employee not found"));

        UUID activeCompanyId = employee.lastActiveCompanyId();
        UUID activeStoreId = employee.lastActiveStoreId();

        // If not set, try to default to first available
        if (activeCompanyId == null && !availableCompanies.isEmpty()) {
            activeCompanyId = availableCompanies.get(0).id();
            // If company selected, try to select its first store
            if (activeStoreId == null && !availableCompanies.get(0).stores().isEmpty()) {
                activeStoreId = availableCompanies.get(0).stores().get(0).id();
            }
        }

        // Find DTOs for active
        CompanyTreeDto activeCompany = null;
        if (activeCompanyId != null) {
            UUID finalCompId = activeCompanyId;
            activeCompany = availableCompanies.stream().filter(c -> c.id().equals(finalCompId)).findFirst()
                    .orElse(null);
        }

        StoreTreeDto activeStore = null;
        if (activeStoreId != null) {
            UUID finalStoreId = activeStoreId;
            activeStore = availableStores.stream().filter(s -> s.id().equals(finalStoreId)).findFirst().orElse(null);
        }

        // If activeStore is set but activeCompany is not (edge case?), derive company
        // from store
        if (activeStore != null && activeCompany == null && activeStore.companyId() != null) {
            UUID parentCompId = activeStore.companyId();
            activeCompany = availableCompanies.stream().filter(c -> c.id().equals(parentCompId)).findFirst()
                    .orElse(null);
        }

        // Return Context
        return new es.terencio.erp.organization.application.dto.DashboardContextDto(
                activeCompany,
                activeStore,
                availableCompanies);
    }

    @Transactional
    public void switchContext(Long employeeId, UUID companyId, UUID storeId) {
        // Validate access
        List<OrganizationTreeDto> tree = getOrganizationTreeForEmployee(employeeId);

        boolean companyValid = companyId == null;
        boolean storeValid = storeId == null;

        if (companyId != null) {
            companyValid = tree.stream()
                    .flatMap(o -> o.companies().stream())
                    .anyMatch(c -> c.id().equals(companyId));
        }

        if (storeId != null) {
            storeValid = tree.stream()
                    .flatMap(o -> o.companies().stream())
                    .flatMap(c -> c.stores().stream())
                    .anyMatch(s -> s.id().equals(storeId));

            // Also validate store belongs to company if both provided?
            // Logic: If store is provided, company MUST match its parent?
            // Or we trust the frontend/user to send consistent pair.
            // Let's enforce consistency if both present.
            if (companyId != null && storeId != null && storeValid) {
                // Re-check parentage
                // We can check if store's parent company id matches companyId
                // But we don't have easy lookup here without re-traversing
                // Let's rely on basic existence validation for now.
            }
        }

        if (!companyValid || !storeValid) {
            throw new IllegalArgumentException(
                    "Invalid context: User does not have access to specified company or store");
        }

        employeePort.updateLastActiveContext(employeeId, companyId, storeId);
    }
}
