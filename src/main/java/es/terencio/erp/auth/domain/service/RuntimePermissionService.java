package es.terencio.erp.auth.domain.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  /**
   * Checks if an employee has a specific permission for a given target and scope.
   * Hierarchy resolution is done in SQL — an ORGANIZATION-level grant covers all
   * companies and stores within it; a COMPANY-level grant covers all stores
   * within it.
   */
  public boolean hasPermission(Long employeeId, String permissionCode, UUID targetId, AccessScope scope) {
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
                        (g.scope = 'ORGANIZATION'
                         AND g.target_id =
                             COALESCE(
                                 c_from_store.organization_id,
                                 c_direct.organization_id,
                                 :targetId
                             )
                        )

                     OR (g.scope = 'COMPANY'
                         AND g.target_id =
                             COALESCE(
                                 s.company_id,
                                 :targetId
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
        .query(Boolean.class)
        .single();
  }

  /**
   * Returns a permission matrix for the given employee grouped by scope → target
   * UUID → permissions.
   * Example:
   * {@code { "COMPANY": { uuid: ["product:create"] }, "STORE": { uuid: ["organization:store:view"] } }}
   */
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

          matrix
              .computeIfAbsent(scopeKey, k -> new LinkedHashMap<>())
              .computeIfAbsent(targetId, k -> new ArrayList<>())
              .add(permCode);
          return null;
        })
        .list();

    return matrix;
  }
}
