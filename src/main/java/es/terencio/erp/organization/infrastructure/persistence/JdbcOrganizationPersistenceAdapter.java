package es.terencio.erp.organization.infrastructure.persistence;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.terencio.erp.organization.application.dto.CompanyTreeDto;
import es.terencio.erp.organization.application.dto.OrganizationTreeDto;
import es.terencio.erp.organization.application.dto.StoreTreeDto;
import es.terencio.erp.organization.application.port.out.OrganizationRepository;

@Repository
public class JdbcOrganizationPersistenceAdapter implements OrganizationRepository {

    private final JdbcClient jdbcClient;

    public JdbcOrganizationPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<OrganizationTreeDto> findOrganizationsByIds(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return jdbcClient.sql("SELECT id, name, slug FROM organizations WHERE id IN (:ids) ORDER BY name")
                .param("ids", ids)
                .query((rs, rowNum) -> new OrganizationTreeDto(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("slug"),
                        null)) // Companies will be populated by service
                .list();
    }

    @Override
    public List<CompanyTreeDto> findCompaniesByIds(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return jdbcClient.sql("SELECT id, name, slug, organization_id FROM companies WHERE id IN (:ids) ORDER BY name")
                .param("ids", ids)
                .query((rs, rowNum) -> new CompanyTreeDto(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("slug"),
                        rs.getObject("organization_id", UUID.class),
                        null)) // Stores populate by service
                .list();
    }

    @Override
    public List<StoreTreeDto> findStoresByIds(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return jdbcClient.sql("SELECT id, name, slug, code, company_id FROM stores WHERE id IN (:ids) ORDER BY name")
                .param("ids", ids)
                .query((rs, rowNum) -> new StoreTreeDto(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("slug"),
                        rs.getString("code"),
                        rs.getObject("company_id", UUID.class)))
                .list();
    }

    @Override
    public List<CompanyTreeDto> findCompaniesByOrganizationIds(Set<UUID> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return Collections.emptyList();
        }
        return jdbcClient.sql(
                "SELECT id, name, slug, organization_id FROM companies WHERE organization_id IN (:ids) ORDER BY name")
                .param("ids", organizationIds)
                .query((rs, rowNum) -> new CompanyTreeDto( // We might need orgId to map back, but for now strict tree
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("slug"),
                        rs.getObject("organization_id", UUID.class),
                        null))
                .list();
    }

    @Override
    public List<StoreTreeDto> findStoresByCompanyIds(Set<UUID> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Collections.emptyList();
        }
        return jdbcClient
                .sql("SELECT id, name, slug, code, company_id FROM stores WHERE company_id IN (:ids) ORDER BY name")
                .param("ids", companyIds)
                .query((rs, rowNum) -> new StoreTreeDto(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("slug"),
                        rs.getString("code"),
                        rs.getObject("company_id", UUID.class)))
                .list();
    }

    // NOTE: To correctly assemble the tree, we might need parent IDs in the DTOs or
    // separate mapping methods.
    // The Service will handle the assembly, but retrieving parent IDs is helpful.
    // For now the Service will likely fetch Parents logic to know what to fetch.

    // Actually, to make the service simpler, I might need methods to find Parent
    // IDs given Child IDs.
    @Override
    public List<UUID> findOrganizationIdsByCompanyIds(Set<UUID> companyIds) {
        if (companyIds.isEmpty())
            return Collections.emptyList();
        return jdbcClient.sql("SELECT DISTINCT organization_id FROM companies WHERE id IN (:ids)")
                .param("ids", companyIds)
                .query(UUID.class)
                .list();
    }

    @Override
    public List<UUID> findCompanyIdsByStoreIds(Set<UUID> storeIds) {
        if (storeIds.isEmpty())
            return Collections.emptyList();
        return jdbcClient.sql("SELECT DISTINCT company_id FROM stores WHERE id IN (:ids)")
                .param("ids", storeIds)
                .query(UUID.class)
                .list();
    }
}
