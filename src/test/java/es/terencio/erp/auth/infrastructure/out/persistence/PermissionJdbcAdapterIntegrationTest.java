package es.terencio.erp.auth.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.AbstractIntegrationTest;

@Transactional
public class PermissionJdbcAdapterIntegrationTest extends AbstractIntegrationTest {

    @Autowired private PermissionJdbcAdapter permissionAdapter;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long employeeId = 999L;
    private UUID organizationId = UUID.randomUUID(), companyId = UUID.randomUUID(), storeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cleanDatabase();
        jdbcTemplate.update("INSERT INTO organizations (id, name) VALUES (?, ?)", organizationId, "Org1");
        jdbcTemplate.update("INSERT INTO companies (id, name, tax_id, organization_id) VALUES (?, ?, ?, ?)", companyId, "Comp1", "B1", organizationId);
        jdbcTemplate.update("INSERT INTO stores (id, name, code, company_id) VALUES (?, ?, ?, ?)", storeId, "Store1", "S1", companyId);
        jdbcTemplate.update("INSERT INTO employees (id, username, password_hash, full_name, uuid, is_active, organization_id) VALUES (?, ?, ?, ?, ?, ?, ?)", employeeId, "test", "hash", "Test", UUID.randomUUID(), true, organizationId);
        jdbcTemplate.update("INSERT INTO permissions (code, name, module) VALUES (?, ?, ?)", "marketing:campaign:create", "Create", "MARKETING");
        jdbcTemplate.update("INSERT INTO roles (name) VALUES (?)", "MANAGER");
        jdbcTemplate.update("INSERT INTO role_permissions (role_name, permission_code) VALUES (?, ?)", "MANAGER", "marketing:campaign:create");
    }

    @Test
    void testPermission_WhenGrantExistsForCompany_ShouldReturnTrue() {
        jdbcTemplate.update("INSERT INTO employee_access_grants (employee_id, role, scope, target_id, created_at) VALUES (?, ?, ?, ?, ?)", employeeId, "MANAGER", "COMPANY", companyId, java.sql.Timestamp.from(Instant.now()));
        assertThat(permissionAdapter.hasPermission(employeeId, "marketing:campaign:create", companyId, AccessScope.COMPANY)).isTrue();
    }
}
