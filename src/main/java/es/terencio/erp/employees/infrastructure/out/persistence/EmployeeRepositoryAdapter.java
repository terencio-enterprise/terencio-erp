package es.terencio.erp.employees.infrastructure.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.terencio.erp.auth.domain.model.AccessGrant;
import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.employees.application.dto.EmployeeDto;
import es.terencio.erp.employees.application.dto.EmployeeSyncDto;
import es.terencio.erp.employees.application.port.out.EmployeePort;

@Repository
public class EmployeeRepositoryAdapter implements EmployeePort {
    
    private final JdbcClient jdbcClient;

    public EmployeeRepositoryAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<EmployeeDto> findAll() {
        return jdbcClient.sql("SELECT id, uuid, organization_id, username, email, full_name, is_active, last_active_company_id, last_active_store_id, created_at, updated_at FROM employees")
            .query((rs, rowNum) -> new EmployeeDto(
                rs.getLong("id"), rs.getObject("uuid", UUID.class), rs.getObject("organization_id", UUID.class),
                rs.getString("username"), rs.getString("email"), rs.getString("full_name"), rs.getBoolean("is_active"),
                List.of(), rs.getObject("last_active_company_id", UUID.class), rs.getObject("last_active_store_id", UUID.class),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()
            )).list();
    }

    @Override
    public Optional<EmployeeDto> findById(Long id) {
        return jdbcClient.sql("SELECT id, uuid, organization_id, username, email, full_name, is_active, last_active_company_id, last_active_store_id, created_at, updated_at FROM employees WHERE id = :id")
            .param("id", id)
            .query((rs, rowNum) -> new EmployeeDto(
                rs.getLong("id"), rs.getObject("uuid", UUID.class), rs.getObject("organization_id", UUID.class),
                rs.getString("username"), rs.getString("email"), rs.getString("full_name"), rs.getBoolean("is_active"),
                List.of(), rs.getObject("last_active_company_id", UUID.class), rs.getObject("last_active_store_id", UUID.class),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()
            )).optional();
    }

    @Override
    public Optional<EmployeeDto> findByUsername(String username) {
         return jdbcClient.sql("SELECT id, uuid, organization_id, username, email, full_name, is_active, last_active_company_id, last_active_store_id, created_at, updated_at FROM employees WHERE username = :username")
            .param("username", username)
            .query((rs, rowNum) -> new EmployeeDto(
                rs.getLong("id"), rs.getObject("uuid", UUID.class), rs.getObject("organization_id", UUID.class),
                rs.getString("username"), rs.getString("email"), rs.getString("full_name"), rs.getBoolean("is_active"),
                List.of(), rs.getObject("last_active_company_id", UUID.class), rs.getObject("last_active_store_id", UUID.class),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()
            )).optional();
    }

    @Override
    public Long save(UUID organizationId, String username, String email, String fullName, String pinHash, String passwordHash) {
        return jdbcClient.sql("INSERT INTO employees (organization_id, username, email, full_name, pin_hash, password_hash) VALUES (:orgId, :username, :email, :fullName, :pinHash, :pwdHash) RETURNING id")
            .param("orgId", organizationId).param("username", username).param("email", email)
            .param("fullName", fullName).param("pinHash", pinHash).param("pwdHash", passwordHash)
            .query(Long.class).single();
    }

    @Override
    public void update(Long id, String fullName, String email, boolean isActive) {
        jdbcClient.sql("UPDATE employees SET full_name = :fullName, email = :email, is_active = :isActive, updated_at = NOW() WHERE id = :id")
            .param("fullName", fullName).param("email", email).param("isActive", isActive).param("id", id).update();
    }

    @Override
    public void updatePin(Long id, String pinHash) {
        jdbcClient.sql("UPDATE employees SET pin_hash = :pinHash, updated_at = NOW() WHERE id = :id")
            .param("pinHash", pinHash).param("id", id).update();
    }

    @Override
    public void updatePassword(Long id, String passwordHash) {
        jdbcClient.sql("UPDATE employees SET password_hash = :passwordHash, updated_at = NOW() WHERE id = :id")
            .param("passwordHash", passwordHash).param("id", id).update();
    }

    @Override
    public void syncAccessGrants(Long id, String role, UUID companyId, UUID storeId) {
        jdbcClient.sql("DELETE FROM employee_access_grants WHERE employee_id = :id").param("id", id).update();
        if (storeId != null) {
            jdbcClient.sql("INSERT INTO employee_access_grants (employee_id, scope, target_id, role) VALUES (:id, 'STORE', :targetId, :role)")
                .param("id", id).param("targetId", storeId).param("role", role).update();
        } else if (companyId != null) {
            jdbcClient.sql("INSERT INTO employee_access_grants (employee_id, scope, target_id, role) VALUES (:id, 'COMPANY', :targetId, :role)")
                .param("id", id).param("targetId", companyId).param("role", role).update();
        }
    }

    @Override
    public List<EmployeeSyncDto> findSyncDataByStoreId(UUID storeId) {
        return jdbcClient.sql("SELECT e.id, e.username, e.full_name, g.role, e.pin_hash, e.last_active_company_id, e.last_active_store_id FROM employees e JOIN employee_access_grants g ON e.id = g.employee_id WHERE g.target_id = :storeId AND e.is_active = TRUE")
            .param("storeId", storeId)
            .query((rs, rowNum) -> new EmployeeSyncDto(
                rs.getLong("id"), rs.getString("username"), rs.getString("full_name"), rs.getString("role"),
                rs.getString("pin_hash"), rs.getObject("last_active_company_id", UUID.class), rs.getObject("last_active_store_id", UUID.class)
            )).list();
    }

    @Override
    public List<AccessGrant> findAccessGrants(Long employeeId) {
        return jdbcClient.sql("SELECT scope, target_id, role FROM employee_access_grants WHERE employee_id = :empId")
            .param("empId", employeeId)
            .query((rs, rowNum) -> new AccessGrant(
                AccessScope.valueOf(rs.getString("scope")), rs.getObject("target_id", UUID.class), rs.getString("role"),
                java.util.Set.of(), java.util.Set.of()
            )).list();
    }

    @Override
    public void updateLastActiveContext(Long employeeId, UUID companyId, UUID storeId) {
        jdbcClient.sql("UPDATE employees SET last_active_company_id = :companyId, last_active_store_id = :storeId WHERE id = :id")
            .param("companyId", companyId).param("storeId", storeId).param("id", employeeId).update();
    }
}
