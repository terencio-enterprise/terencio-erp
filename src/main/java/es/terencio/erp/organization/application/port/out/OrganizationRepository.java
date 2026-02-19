package es.terencio.erp.organization.application.port.out;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import es.terencio.erp.organization.application.dto.OrganizationDtos.CompanyTreeDto;
import es.terencio.erp.organization.application.dto.OrganizationDtos.OrganizationTreeDto;
import es.terencio.erp.organization.application.dto.OrganizationDtos.StoreTreeDto;

public interface OrganizationRepository {
    List<OrganizationTreeDto> findOrganizationsByIds(Set<UUID> ids);
    List<CompanyTreeDto> findCompaniesByIds(Set<UUID> ids);
    List<StoreTreeDto> findStoresByIds(Set<UUID> ids);
    List<CompanyTreeDto> findCompaniesByOrganizationIds(Set<UUID> organizationIds);
    List<StoreTreeDto> findStoresByCompanyIds(Set<UUID> companyIds);
    List<UUID> findOrganizationIdsByCompanyIds(Set<UUID> companyIds);
    List<UUID> findCompanyIdsByStoreIds(Set<UUID> storeIds);
}
