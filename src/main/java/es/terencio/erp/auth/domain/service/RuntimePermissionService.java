package es.terencio.erp.auth.domain.service;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import es.terencio.erp.auth.domain.model.AccessScope;

@Service
public class RuntimePermissionService {

    private final JdbcClient jdbcClient;

    public RuntimePermissionService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public boolean hasPermission(Long employeeId, String permissionCode, UUID targetId, AccessScope scope) {
        // The prompt provided a specific SQL query logic.
        // It relies on finding an existing grant that matches the criteria.
        // Hierarchy resolution is done inside SQL.

        String sql = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM employee_access_grants g
                        JOIN role_permissions rp
                          ON rp.role_name = g.role

                        LEFT JOIN stores s
                          ON s.id = :targetId
                        LEFT JOIN companies c_from_store
                          ON c_from_store.id = s.company_id
                        LEFT JOIN companies c_direct
                          ON c_direct.id = :targetId

                        WHERE g.employee_id = :employeeId
                          AND rp.permission_code = :permissionCode
                          AND (
                                -- Scope: ORGANIZATION
                                -- Grant at ORG level covers everything in that ORG.
                                -- We need to check if the target's organization matches the grant's target_id.
                                (g.scope = 'ORGANIZATION'
                                 AND g.target_id =
                                     COALESCE(
                                         c_from_store.organization_id, -- If target is STORE, get its company's org
                                         c_direct.organization_id,     -- If target is COMPANY, get its org
                                         :targetId                     -- If target is ORG (implied), or fallback
                                     )
                                )

                             OR (g.scope = 'COMPANY'
                                 AND g.target_id =
                                     COALESCE(
                                         s.company_id, -- If target is STORE, get its company
                                         :targetId     -- If target is COMPANY, match directly
                                     )
                                )

                             OR (g.scope = 'STORE'
                                 AND g.target_id = :targetId)
                          )
                    )
                """;

        return jdbcClient.sql(sql)
                .param("employeeId", employeeId)
                .param("permissionCode", permissionCode)
                .param("targetId", targetId)
                // We don't strictly need 'scope' param in SQL if we assume the checks align
                // with the logic provided.
                // The revised SQL covers hierarchy dynamically.
                // However, the provided SQL in prompt didn't explicitly use 'scope' parameter,
                // it used g.scope column.
                .query(Boolean.class)
                .single();
    }
}
