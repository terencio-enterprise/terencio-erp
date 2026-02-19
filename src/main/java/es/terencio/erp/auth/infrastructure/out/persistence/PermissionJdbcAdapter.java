package es.terencio.erp.auth.infrastructure.out.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import es.terencio.erp.auth.application.port.out.PermissionPort;
import es.terencio.erp.auth.domain.model.AccessScope;

@Repository
public class PermissionJdbcAdapter implements PermissionPort {

    private final JdbcClient jdbcClient;

    public PermissionJdbcAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean hasPermission(Long employeeId, String permissionCode, UUID targetId, AccessScope scope) {
        String sql = """
            SELECT EXISTS (
                SELECT 1
                FROM employee_access_grants g
                JOIN role_permissions rp ON rp.role_name = g.role
                LEFT JOIN stores s ON s.id = :targetId
                LEFT JOIN companies c_from_store ON c_from_store.id = s.company_id
                LEFT JOIN companies c_direct ON c_direct.id = :targetId
                WHERE g.employee_id = :employeeId
                  AND rp.permission_code = :permissionCode
                  AND (
                        (g.scope = 'ORGANIZATION' AND g.target_id = COALESCE(c_from_store.organization_id, c_direct.organization_id, :targetId))
                     OR (g.scope = 'COMPANY' AND g.target_id = COALESCE(s.company_id, :targetId))
                     OR (g.scope = 'STORE' AND g.target_id = :targetId)
                  )
            )
        """;

        return jdbcClient.sql(sql)
            .param("employeeId", employeeId)
            .param("permissionCode", permissionCode)
            .param("targetId", targetId)
            .query(Boolean.class)
            .single();
    }

    @Override
    public Map<String, Map<UUID, List<String>>> getPermissionMatrix(Long employeeId) {
        String sql = """
            SELECT g.scope, g.target_id, rp.permission_code
            FROM employee_access_grants g
            JOIN role_permissions rp ON rp.role_name = g.role
            WHERE g.employee_id = :employeeId
            ORDER BY g.scope, g.target_id, rp.permission_code
        """;

        Map<String, Map<UUID, List<String>>> matrix = new LinkedHashMap<>();

        jdbcClient.sql(sql)
            .param("employeeId", employeeId)
            .query((rs, rowNum) -> {
                String scopeKey = rs.getString("scope");
                UUID targetId = (UUID) rs.getObject("target_id");
                String permCode = rs.getString("permission_code");

                matrix.computeIfAbsent(scopeKey, k -> new LinkedHashMap<>())
                      .computeIfAbsent(targetId, k -> new ArrayList<>())
                      .add(permCode);
                return null;
            })
            .list();

        return matrix;
    }
}
