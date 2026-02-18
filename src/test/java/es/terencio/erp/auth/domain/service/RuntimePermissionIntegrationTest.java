package es.terencio.erp.auth.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.auth.domain.model.AccessScope;

@Transactional
public class RuntimePermissionIntegrationTest extends es.terencio.erp.AbstractIntegrationTest {

        @Autowired
        private RuntimePermissionService permissionService;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        private Long employeeId;
        private UUID organizationId;
        private UUID companyId;
        private UUID storeId;

        @BeforeEach
        void setUp() {
                cleanDatabase();

                // Create Organizations, Companies, Stores hierarchy
                organizationId = UUID.randomUUID();
                companyId = UUID.randomUUID();
                storeId = UUID.randomUUID();

                // Assuming migrations run, we have organization_id in companies and employees
                jdbcTemplate.update("INSERT INTO organizations (id, name) VALUES (?, ?)", organizationId, "Org1");
                jdbcTemplate.update("INSERT INTO companies (id, name, tax_id, organization_id) VALUES (?, ?, ?, ?)",
                                companyId, "Comp1", "B12345678", organizationId);
                jdbcTemplate.update("INSERT INTO stores (id, name, code, company_id) VALUES (?, ?, ?, ?)", storeId,
                                "Store1", "STORE-001", companyId);

                // Create Employee
                employeeId = 999L;
                // Correct columns: uuid, username, password_hash, full_name, is_active,
                // organization_id
                jdbcTemplate.update(
                                "INSERT INTO employees (id, username, password_hash, full_name, uuid, is_active, organization_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
                                employeeId, "testuser", "pass_hash", "Test User", UUID.randomUUID(), true,
                                organizationId);

                // Create Permissions
                // Permissions table: code, name (NN), description, module (NN)
                jdbcTemplate.update(
                                "INSERT INTO permissions (code, name, description, module) VALUES (?, ?, ?, ?) ON CONFLICT (code) DO NOTHING",
                                "marketing:campaign:create", "Create Campaign", "Create Campaign Description",
                                "MARKETING");
                jdbcTemplate.update(
                                "INSERT INTO permissions (code, name, description, module) VALUES (?, ?, ?, ?) ON CONFLICT (code) DO NOTHING",
                                "marketing:campaign:view", "View Campaign", "View Campaign Description", "MARKETING");

                // Create Roles
                jdbcTemplate.update("INSERT INTO roles (name, description) VALUES (?, ?) ON CONFLICT (name) DO NOTHING",
                                "MARKETING_MANAGER",
                                "Marketing Manager");
                jdbcTemplate.update("INSERT INTO roles (name, description) VALUES (?, ?) ON CONFLICT (name) DO NOTHING",
                                "STORE_MANAGER",
                                "Store Manager");

                // Map Role -> Permission
                jdbcTemplate.update(
                                "INSERT INTO role_permissions (role_name, permission_code) VALUES (?, ?) ON CONFLICT DO NOTHING",
                                "MARKETING_MANAGER", "marketing:campaign:create");
                jdbcTemplate.update(
                                "INSERT INTO role_permissions (role_name, permission_code) VALUES (?, ?) ON CONFLICT DO NOTHING",
                                "MARKETING_MANAGER", "marketing:campaign:view");
                jdbcTemplate.update(
                                "INSERT INTO role_permissions (role_name, permission_code) VALUES (?, ?) ON CONFLICT DO NOTHING",
                                "STORE_MANAGER", "marketing:campaign:view");
        }

        @Test
        void testPermission_WhenGrantExistsForCompany_ShouldReturnTrue() {
                // Grant MARKETING_MANAGER to employee for companyId
                // employee_access_grants: employee_id, role, scope, target_id, created_at (NO
                // created_by)
                jdbcTemplate.update(
                                "INSERT INTO employee_access_grants (employee_id, role, scope, target_id, created_at) VALUES (?, ?, ?, ?, ?)",
                                employeeId, "MARKETING_MANAGER", "COMPANY", companyId,
                                java.sql.Timestamp.from(Instant.now()));

                // Check permission on companyId
                boolean hasPerm = permissionService.hasPermission(employeeId, "marketing:campaign:create", companyId,
                                AccessScope.COMPANY);
                assertThat(hasPerm).isTrue();

                // Check unrelated permission
                boolean hasUnrelated = permissionService.hasPermission(employeeId, "other:perm", companyId,
                                AccessScope.COMPANY);
                assertThat(hasUnrelated).isFalse();
        }

        @Test
        void testPermission_WhenGrantExistsForOrganization_ShouldCascadeToCompany() {
                // Grant MARKETING_MANAGER to employee for organizationId
                jdbcTemplate.update(
                                "INSERT INTO employee_access_grants (employee_id, role, scope, target_id, created_at) VALUES (?, ?, ?, ?, ?)",
                                employeeId, "MARKETING_MANAGER", "ORGANIZATION", organizationId,
                                java.sql.Timestamp.from(Instant.now()));

                // Check permission on companyId (should inherit from Org)
                boolean hasPerm = permissionService.hasPermission(employeeId, "marketing:campaign:create", companyId,
                                AccessScope.COMPANY);
                assertThat(hasPerm).isTrue();
        }

        @Test
        void testPermission_WhenGrantExistsForStore_ShouldNotAllowCompanyAccess() {
                // Grant STORE_MANAGER to employee for storeId
                jdbcTemplate.update(
                                "INSERT INTO employee_access_grants (employee_id, role, scope, target_id, created_at) VALUES (?, ?, ?, ?, ?)",
                                employeeId, "STORE_MANAGER", "STORE", storeId, java.sql.Timestamp.from(Instant.now()));

                // Check permission on companyId (should FAIL, store is child of company, not
                // parent)
                boolean hasPerm = permissionService.hasPermission(employeeId, "marketing:campaign:view", companyId,
                                AccessScope.COMPANY);
                assertThat(hasPerm).isFalse();

                // Check permission on storeId (should PASS)
                boolean hasStorePerm = permissionService.hasPermission(employeeId, "marketing:campaign:view", storeId,
                                AccessScope.STORE);
                assertThat(hasStorePerm).isTrue();
        }

        @Test
        void testPermission_WhenNoGrant_ShouldReturnFalse() {
                boolean hasPerm = permissionService.hasPermission(employeeId, "marketing:campaign:create", companyId,
                                AccessScope.COMPANY);
                assertThat(hasPerm).isFalse();
        }
}
