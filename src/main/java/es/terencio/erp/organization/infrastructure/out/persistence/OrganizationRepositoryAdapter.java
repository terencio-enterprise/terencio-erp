package es.terencio.erp.organization.infrastructure.out.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import es.terencio.erp.organization.application.dto.OrganizationDtos.CompanyTreeDto;
import es.terencio.erp.organization.application.dto.OrganizationDtos.StoreTreeDto;
import es.terencio.erp.organization.application.port.out.OrganizationRepository;

@Repository
public class OrganizationRepositoryAdapter implements OrganizationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OrganizationRepositoryAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<CompanyTreeDto> findFullTreeByEmployeeId(Long employeeId) {
        // Query fetches all active companies and their active stores for the employee's organization
        String sql = "SELECT " +
                     "    c.id AS company_id, c.name AS company_name, c.slug AS company_slug, c.organization_id, " +
                     "    s.id AS store_id, s.name AS store_name, s.slug AS store_slug, s.code AS store_code " +
                     "FROM companies c " +
                     "JOIN employees e ON c.organization_id = e.organization_id " +
                     "LEFT JOIN stores s ON c.id = s.company_id AND s.deleted_at IS NULL AND s.is_active = TRUE " +
                     "WHERE e.id = :employeeId " +
                     "  AND c.deleted_at IS NULL " +
                     "  AND c.is_active = TRUE " +
                     "ORDER BY c.name ASC, s.name ASC";

        return jdbcTemplate.query(sql, new MapSqlParameterSource("employeeId", employeeId), rs -> {
            Map<UUID, CompanyTreeDto> companyMap = new LinkedHashMap<>();
            
            while (rs.next()) {
                UUID companyId = rs.getObject("company_id", UUID.class);
                
                // Initialize the Company node if we haven't seen it yet
                if (!companyMap.containsKey(companyId)) {
                    companyMap.put(companyId, new CompanyTreeDto(
                        companyId,
                        rs.getString("company_name"),
                        rs.getString("company_slug"),
                        rs.getObject("organization_id", UUID.class),
                        new ArrayList<>() // Empty list to hold stores
                    ));
                }
                
                // If a Store exists for this row, add it to the Company's store list
                UUID storeId = rs.getObject("store_id", UUID.class);
                if (storeId != null) {
                    StoreTreeDto storeDto = new StoreTreeDto(
                        storeId,
                        rs.getString("store_name"),
                        rs.getString("store_slug"),
                        rs.getString("store_code"),
                        companyId
                    );
                    companyMap.get(companyId).stores().add(storeDto);
                }
            }
            
            return new ArrayList<>(companyMap.values());
        });
    }
}