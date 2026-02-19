package es.terencio.erp.organization.infrastructure.out.persistence;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import es.terencio.erp.organization.application.dto.OrganizationDtos.CompanyTreeDto;
import es.terencio.erp.organization.application.dto.OrganizationDtos.OrganizationTreeDto;
import es.terencio.erp.organization.application.dto.OrganizationDtos.StoreTreeDto;
import es.terencio.erp.organization.application.port.out.OrganizationRepository;

@Repository
public class JdbcOrganizationPersistenceAdapter implements OrganizationRepository {

    private final JdbcClient jdbcClient;

    public JdbcOrganizationPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<OrganizationTreeDto> findOrganizationsByIds(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return jdbcClient.sql("SELECT id, name, slug FROM organizations WHERE id IN (:ids) ORDER BY name")
                .param("ids", ids).query((rs, rowNum) -> new OrganizationTreeDto(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("slug"), null)).list();
    }

    @Override
    public List<CompanyTreeDto> findCompaniesByIds(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return jdbcClient.sql("SELECT id, name, slug, organization_id FROM companies WHERE id IN (:ids) ORDER BY name")
                .param("ids", ids).query((rs, rowNum) -> new CompanyTreeDto(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("slug"), rs.getObject("organization_id", UUID.class), null)).list();
    }

    @Override
    public List<StoreTreeDto> findStoresByIds(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return jdbcClient.sql("SELECT id, name, slug, code, company_id FROM stores WHERE id IN (:ids) ORDER BY name")
                .param("ids", ids).query((rs, rowNum) -> new StoreTreeDto(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("slug"), rs.getString("code"), rs.getObject("company_id", UUID.class))).list();
    }

    @Override
    public List<CompanyTreeDto> findCompaniesByOrganizationIds(Set<UUID> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) return Collections.emptyList();
        return jdbcClient.sql("SELECT id, name, slug, organization_id FROM companies WHERE organization_id IN (:ids) ORDER BY name")
                .param("ids", organizationIds).query((rs, rowNum) -> new CompanyTreeDto(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("slug"), rs.getObject("organization_id", UUID.class), null)).list();
    }

    @Override
    public List<StoreTreeDto> findStoresByCompanyIds(Set<UUID> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) return Collections.emptyList();
        return jdbcClient.sql("SELECT id, name, slug, code, company_id FROM stores WHERE company_id IN (:ids) ORDER BY name")
                .param("ids", companyIds).query((rs, rowNum) -> new StoreTreeDto(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("slug"), rs.getString("code"), rs.getObject("company_id", UUID.class))).list();
    }

    @Override
    public List<UUID> findOrganizationIdsByCompanyIds(Set<UUID> companyIds) {
        if (companyIds.isEmpty()) return Collections.emptyList();
        return jdbcClient.sql("SELECT DISTINCT organization_id FROM companies WHERE id IN (:ids)").param("ids", companyIds).query(UUID.class).list();
    }

    @Override
    public List<UUID> findCompanyIdsByStoreIds(Set<UUID> storeIds) {
        if (storeIds.isEmpty()) return Collections.emptyList();
        return jdbcClient.sql("SELECT DISTINCT company_id FROM stores WHERE id IN (:ids)").param("ids", storeIds).query(UUID.class).list();
    }
}
