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
                AND e.is_active = TRUE AND e.role != 'ADMIN'
                ORDER BY e.username
                """)
                .param("storeId", storeId).query((rs, rowNum) -> mapRow(rs)).list();
    }

    @Override
    public List<EmployeeSyncDto> findSyncDataByStoreId(UUID storeId) {
        return jdbcClient.sql("""
                SELECT e.id, e.username, e.full_name, e.role, e.pin_hash
                FROM employees e
                JOIN employee_access_grants g ON e.id = g.employee_id
                WHERE g.scope = 'STORE' AND g.target_id = :storeId
                AND e.is_active = TRUE AND e.role != 'ADMIN'
                ORDER BY e.username
                """)
                .param("storeId", storeId)
                .query((rs, rowNum) -> new EmployeeSyncDto(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        rs.getString("pin_hash")))
                .list();
    }

    private EmployeeDto mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new EmployeeDto(
                rs.getLong("id"), rs.getString("username"), rs.getString("full_name"),
                rs.getString("role"), rs.getBoolean("is_active") ? 1 : 0,
                parsePermissions(rs.getString("permissions_json")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    @Override
    public Long save(String username, String fullName, String role, String pinHash, String passwordHash, UUID companyId,
            UUID storeId, String permissionsJson) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient
                .sql("""
                        INSERT INTO employees (username, full_name, role, pin_hash, password_hash, organization_id, permissions_json, is_active, created_at, updated_at)
                        VALUES (:username, :fullName, :role, :pinHash, :passwordHash,
                            (SELECT organization_id FROM companies WHERE id = :companyId),
                            CAST(:permissionsJson AS JSONB), TRUE, NOW(), NOW())
                                RETURNING id
                                """)
                .param("username", username).param("fullName", fullName).param("role", role)
                .param("pinHash", pinHash).param("passwordHash", passwordHash)
                .param("companyId", companyId)
                .param("permissionsJson", permissionsJson).update(keyHolder);

        @SuppressWarnings("null")
        Long generatedId = (Long) keyHolder.getKeys().get("id");

        // Manually handle store grant if provided
        if (storeId != null) {
            insertGrant(generatedId, "STORE", storeId, role);
        }

        return generatedId;
    }

    @Override
    public void syncAccessGrants(Long EmployeeId, String role, UUID companyId, UUID storeId) {
        UUID existingCompanyGrantId = jdbcClient.sql("""
                SELECT target_id
                FROM employee_access_grants
                WHERE employee_id = :id AND scope = 'COMPANY'
                ORDER BY id
                LIMIT 1
                """)
                .param("id", EmployeeId)
                .query((rs, rowNum) -> rs.getObject("target_id", UUID.class))
                .optional()
                .orElse(null);

        jdbcClient.sql("DELETE FROM employee_access_grants WHERE employee_id = :EmployeeId")
                .param("EmployeeId", EmployeeId)
                .update();

        UUID organizationId = jdbcClient.sql("SELECT organization_id FROM employees WHERE id = :id")
                .param("id", EmployeeId)
                .query((rs, rowNum) -> rs.getObject("organization_id", UUID.class))
                .optional()
                .orElse(null);

        UUID effectiveCompanyId = companyId != null ? companyId : existingCompanyGrantId;

        if (storeId != null) {
            insertGrant(EmployeeId, "STORE", storeId, role);
            return;
        }

        if (effectiveCompanyId != null) {
            insertGrant(EmployeeId, "COMPANY", effectiveCompanyId, role);
            return;
        }

        if (organizationId != null) {
            insertGrant(EmployeeId, "ORGANIZATION", organizationId, role);
        }
    }

    private void insertGrant(Long EmployeeId, String scope, UUID targetId, String role) {
        jdbcClient.sql("""
                INSERT INTO employee_access_grants (employee_id, scope, target_id, role, created_at)
                VALUES (:EmployeeId, :scope, :targetId, :role, NOW())
                """)
                .param("EmployeeId", EmployeeId)
                .param("scope", scope)
                .param("targetId", targetId)
                .param("role", role)
                .update();
    }

    @Override
    public void update(Long id, String fullName, String role, UUID storeId, boolean isActive, String permissionsJson) {
        jdbcClient.sql("""
                UPDATE employees SET full_name = :fullName, role = :role,
                    is_active = :isActive, permissions_json = CAST(:permissionsJson AS JSONB), updated_at = NOW()
                WHERE id = :id
                """)
                .param("id", id).param("fullName", fullName).param("role", role)
                .param("isActive", isActive).param("permissionsJson", permissionsJson)
                .update();

        // Update grant if storeId is provided (simplified logic: blindly replace STORE
        // grant)
        if (storeId != null) {
            // Remove existing STORE grants for this user? Or just upsert?
            // For now, let's assume we replace the primary store access.
            // But realistically, update() might not want to touch grants unless explicitly
            // asked.
            // Given the signature includes storeId, it implies we want to set that.

            // However, since we moved to multiple grants, this legacy update might be
            // dangerous.
            // Let's defer grant updates to syncAccessGrants where possible.
            // But to keep legacy behavior: if storeId is NOT NULL, ensure they have access.

            // Check if grant exists
            Integer count = jdbcClient.sql(
                    "SELECT COUNT(*) FROM employee_access_grants WHERE employee_id = :id AND scope = 'STORE' AND target_id = :storeId")
                    .param("id", id).param("storeId", storeId).query(Integer.class).single();

            if (count == 0) {
                insertGrant(id, "STORE", storeId, role);
            }
        }
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

    private List<String> parsePermissions(String json) {
        if (json == null)
            return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
