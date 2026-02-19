package es.terencio.erp.employees.infrastructure.persistence;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.terencio.erp.employees.application.dto.EmployeeDto;
import es.terencio.erp.employees.application.dto.EmployeeSyncDto;
import es.terencio.erp.employees.application.port.out.EmployeePort;

@Repository
public class JdbcEmployeePersistenceAdapter implements EmployeePort {

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public JdbcEmployeePersistenceAdapter(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<EmployeeDto> findAll() {
        return jdbcClient.sql("SELECT * FROM employees ORDER BY username")
                .query((rs, rowNum) -> mapRow(rs)).list();
    }

    @Override
    public Optional<EmployeeDto> findById(Long id) {
        return jdbcClient.sql("SELECT * FROM employees WHERE id = :id")
                .param("id", id).query((rs, rowNum) -> mapRow(rs)).optional();
    }

    @Override
    public Optional<EmployeeDto> findByUsername(String username) {
        return jdbcClient.sql("SELECT * FROM employees WHERE username = :username")
                .param("username", username).query((rs, rowNum) -> mapRow(rs)).optional();
    }

    @Override
    public List<EmployeeDto> findByStoreId(UUID storeId) {
        return jdbcClient.sql("""
                SELECT e.*
                FROM employees e
                JOIN employee_access_grants g ON e.id = g.employee_id
                WHERE g.scope = 'STORE' AND g.target_id = :storeId
                AND e.is_active = TRUE AND g.role != 'ADMIN'
                ORDER BY e.username
                """)
                .param("storeId", storeId).query((rs, rowNum) -> mapRow(rs)).list();
    }

    @Override
    public List<EmployeeSyncDto> findSyncDataByStoreId(UUID storeId) {
        return jdbcClient
                .sql("""
                        SELECT e.id, e.username, e.full_name, g.role, e.pin_hash, e.last_active_company_id, e.last_active_store_id
                        FROM employees e
                        JOIN employee_access_grants g ON e.id = g.employee_id
                        WHERE g.scope = 'STORE' AND g.target_id = :storeId
                        AND e.is_active = TRUE AND g.role != 'ADMIN'
                        ORDER BY e.username
                        """)
                .param("storeId", storeId)
                .query((rs, rowNum) -> new EmployeeSyncDto(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        rs.getString("pin_hash"),
                        rs.getObject("last_active_company_id", UUID.class),
                        rs.getObject("last_active_store_id", UUID.class)))
                .list();
    }

    private EmployeeDto mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Long employeeId = rs.getLong("id");
        // Subquery to get roles derived from grants
        List<String> roles = jdbcClient.sql("SELECT DISTINCT role FROM employee_access_grants WHERE employee_id = ?")
                .param(employeeId).query(String.class).list();

        return new EmployeeDto(
                employeeId,
                (UUID) rs.getObject("uuid"),
                (UUID) rs.getObject("organization_id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("full_name"),
                rs.getBoolean("is_active"),
                roles,
                rs.getObject("last_active_company_id", UUID.class),
                rs.getObject("last_active_store_id", UUID.class),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    @Override
    public Long save(UUID organizationId, String username, String email, String fullName, String pinHash,
            String passwordHash) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient
                .sql("""
                        INSERT INTO employees (organization_id, username, email, full_name, pin_hash, password_hash, is_active, created_at, updated_at)
                        VALUES (:orgId, :username, :email, :fullName, :pinHash, :passwordHash, TRUE, NOW(), NOW())
                        RETURNING id
                        """)
                .param("orgId", organizationId)
                .param("username", username)
                .param("email", email)
                .param("fullName", fullName)
                .param("pinHash", pinHash)
                .param("passwordHash", passwordHash)
                .update(keyHolder);

        @SuppressWarnings("null")
        Long generatedId = (Long) keyHolder.getKeys().get("id");
        return generatedId;
    }

    @Override
    public void syncAccessGrants(Long employeeId, String role, UUID companyId, UUID storeId) {
        jdbcClient.sql("DELETE FROM employee_access_grants WHERE employee_id = :employeeId")
                .param("employeeId", employeeId).update();

        UUID organizationId = jdbcClient.sql("SELECT organization_id FROM employees WHERE id = :id")
                .param("id", employeeId).query(UUID.class).optional().orElse(null);

        if (storeId != null) {
            insertGrant(employeeId, "STORE", storeId, role);
            return;
        }

        if (companyId != null) {
            insertGrant(employeeId, "COMPANY", companyId, role);
            return;
        }

        if (organizationId != null) {
            insertGrant(employeeId, "ORGANIZATION", organizationId, role);
        }
    }

    private void insertGrant(Long employeeId, String scope, UUID targetId, String role) {
        jdbcClient.sql("""
                INSERT INTO employee_access_grants (employee_id, scope, target_id, role, created_at)
                VALUES (:employeeId, :scope, :targetId, :role, NOW())
                """)
                .param("employeeId", employeeId)
                .param("scope", scope)
                .param("targetId", targetId)
                .param("role", role)
                .update();
    }

    @Override
    public void update(Long id, String fullName, String email, boolean isActive) {
        jdbcClient.sql("""
                UPDATE employees SET full_name = :fullName, email = :email, is_active = :isActive, updated_at = NOW()
                WHERE id = :id
                """)
                .param("id", id)
                .param("fullName", fullName)
                .param("email", email)
                .param("isActive", isActive)
                .update();
    }

    @Override
    public void updatePin(Long id, String newPinHash) {
        jdbcClient.sql("UPDATE employees SET pin_hash = :pinHash, updated_at = NOW() WHERE id = :id")
                .param("id", id).param("pinHash", newPinHash).update();
    }

    @Override
    public void updatePassword(Long id, String newPasswordHash) {
        jdbcClient.sql("UPDATE employees SET password_hash = :passwordHash, updated_at = NOW() WHERE id = :id")
                .param("id", id).param("passwordHash", newPasswordHash).update();
    }

    @Override
    public List<es.terencio.erp.auth.domain.model.AccessGrant> findAccessGrants(Long employeeId) {
        return jdbcClient.sql(
                "SELECT scope, target_id, role, extra_permissions, excluded_permissions FROM employee_access_grants WHERE employee_id = :employeeId")
                .param("employeeId", employeeId)
                .query((rs, rowNum) -> new es.terencio.erp.auth.domain.model.AccessGrant(
                        es.terencio.erp.auth.domain.model.AccessScope.valueOf(rs.getString("scope")),
                        rs.getObject("target_id", UUID.class),
                        rs.getString("role"),
                        parsePermissionSet(rs.getString("extra_permissions")),
                        parsePermissionSet(rs.getString("excluded_permissions"))))
                .list();
    }

    private java.util.Set<String> parsePermissionSet(String json) {
        if (json == null)
            return Collections.emptySet();
        try {
            return objectMapper.readValue(json, new TypeReference<java.util.Set<String>>() {
            });
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    @Override
    public void updateLastActiveContext(Long id, UUID companyId, UUID storeId) {
        jdbcClient.sql(
                "UPDATE employees SET last_active_company_id = :companyId, last_active_store_id = :storeId, updated_at = NOW() WHERE id = :id")
                .param("id", id)
                .param("companyId", companyId)
                .param("storeId", storeId)
                .update();
    }
}