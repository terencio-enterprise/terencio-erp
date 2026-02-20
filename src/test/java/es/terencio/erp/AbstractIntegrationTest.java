package es.terencio.erp;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import es.terencio.erp.auth.application.dto.AuthDtos.LoginRequest;
import es.terencio.erp.auth.application.dto.AuthDtos.LoginResponse;
import es.terencio.erp.config.TestSecurityConfig;
import es.terencio.erp.shared.presentation.ApiResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public abstract class AbstractIntegrationTest {

        @Autowired
        protected JdbcClient jdbcClient;

        @Autowired
        protected org.springframework.boot.test.web.client.TestRestTemplate restTemplate;

        @Autowired
        protected org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

        static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
                        DockerImageName.parse("postgres:17-alpine"));

        static {
                postgres.start();
        }

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl);
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);
        }

        // ==========================================
        // GLOBAL SHARED TEST DATA
        // ==========================================
        private static boolean globalDataInitialized = false;
        protected static UUID globalOrgId;
        protected static UUID globalCompanyId;
        protected static UUID globalStoreId;
        protected static Long globalAdminId;
        protected static HttpHeaders globalAdminHeaders;
        protected static String globalAdminToken;

        @BeforeEach
        public void setupGlobalState() {
                if (!globalDataInitialized) {
                        // 1. Wipe EVERYTHING once at the very start of the test suite
                        cleanEntireDatabase();

                        // 2. Setup standard global data to be reused across all tests
                        globalOrgId = UUID.randomUUID();
                        jdbcClient.sql(
                                        "INSERT INTO organizations (id, name, slug, subscription_plan) VALUES (:id, 'Global Test Org', 'global-test-org', 'STANDARD')")
                                        .param("id", globalOrgId).update();

                        globalCompanyId = UUID.randomUUID();
                        jdbcClient.sql(
                                        "INSERT INTO companies (id, organization_id, name, slug, tax_id, currency_code, fiscal_regime, price_includes_tax, rounding_mode, is_active, created_at, updated_at, version) VALUES (:id, :orgId, 'Global Test Company', 'global-test-co', 'B11111111', 'EUR', 'COMMON', TRUE, 'LINE', TRUE, NOW(), NOW(), 1)")
                                        .param("id", globalCompanyId).param("orgId", globalOrgId).update();

                        globalStoreId = UUID.randomUUID();
                        jdbcClient.sql(
                                        "INSERT INTO stores (id, company_id, code, name, slug, address, is_active) VALUES (:id, :companyId, 'GLOBAL-STORE', 'Global Test Store', 'global-test-store', 'Global Test Address', TRUE)")
                                        .param("id", globalStoreId).param("companyId", globalCompanyId).update();

                        jdbcClient.sql(
                                        "INSERT INTO store_settings (store_id, allow_negative_stock, print_ticket_automatically) VALUES (:storeId, FALSE, TRUE)")
                                        .param("storeId", globalStoreId).update();

                        // Setup Roles & Permissions FIRST to avoid FK violations
                        jdbcClient.sql(
                                        "INSERT INTO roles (name, description) VALUES ('ADMIN', 'Administrator') ON CONFLICT DO NOTHING")
                                        .update();
                        jdbcClient.sql(
                                        "INSERT INTO roles (name, description) VALUES ('MARKETING_MANAGER', 'Marketing Manager') ON CONFLICT DO NOTHING")
                                        .update();

                        String[] permissions = {
                                        "customer:view", "customer:create", "customer:update", "customer:delete",
                                        "marketing:campaign:view", "marketing:campaign:launch",
                                        "marketing:campaign:create",
                                        "marketing:email:preview", "marketing:template:view",
                                        "marketing:template:create",
                                        "marketing:template:edit", "marketing:template:delete",
                                        "device:view", "device:manage",
                                        "organization:company:view", "organization:company:create",
                                        "organization:company:update",
                                        "organization:company:delete",
                                        "organization:store:view", "organization:store:create",
                                        "organization:store:update",
                                        "organization:store:delete",
                                        "employee:view", "employee:create", "employee:update", "employee:delete"
                        };

                        for (String perm : permissions) {
                                jdbcClient.sql(
                                                "INSERT INTO permissions (code, name, description, module) VALUES (:code, :code, 'desc', 'SYSTEM') ON CONFLICT (code) DO NOTHING")
                                                .param("code", perm).update();
                                jdbcClient.sql(
                                                "INSERT INTO role_permissions (role_name, permission_code) VALUES ('ADMIN', :code) ON CONFLICT DO NOTHING")
                                                .param("code", perm).update();
                                jdbcClient.sql(
                                                "INSERT INTO role_permissions (role_name, permission_code) VALUES ('MARKETING_MANAGER', :code) ON CONFLICT DO NOTHING")
                                                .param("code", perm).update();
                        }

                        // Create global admin
                        String encodedPassword = passwordEncoder.encode("admin123");
                        globalAdminId = jdbcClient.sql(
                                        "INSERT INTO employees (username, full_name, pin_hash, password_hash, organization_id, is_active, created_at, updated_at) VALUES ('admin', 'Global Admin', 'pin123', :password, :orgId, TRUE, NOW(), NOW()) RETURNING id")
                                        .param("password", encodedPassword).param("orgId", globalOrgId)
                                        .query(Long.class).single();

                        jdbcClient.sql(
                                        "INSERT INTO employee_access_grants (employee_id, scope, target_id, role, created_at) VALUES (:employeeId, 'COMPANY', :targetId, 'ADMIN', NOW())")
                                        .param("employeeId", globalAdminId).param("targetId", globalCompanyId).update();
                        jdbcClient.sql(
                                        "INSERT INTO employee_access_grants (employee_id, scope, target_id, role, created_at) VALUES (:employeeId, 'STORE', :targetId, 'ADMIN', NOW())")
                                        .param("employeeId", globalAdminId).param("targetId", globalStoreId).update();
                        jdbcClient.sql(
                                        "INSERT INTO employee_access_grants (employee_id, scope, target_id, role, created_at) VALUES (:employeeId, 'ORGANIZATION', :targetId, 'ADMIN', NOW())")
                                        .param("employeeId", globalAdminId).param("targetId", globalOrgId).update();

                        globalDataInitialized = true;
                        globalAdminHeaders = loginAndGetHeaders("admin", "admin123");
                } else {
                        // 3. For subsequent tests, ONLY wipe transactional data to prevent test
                        // pollution
                        cleanTransactionalTables();
                }
        }

        private void cleanEntireDatabase() {
                cleanTransactionalTables();

                // Wipe structural/reference tables only once
                jdbcClient.sql("DELETE FROM role_permissions").update();
                jdbcClient.sql("DELETE FROM roles").update();
                jdbcClient.sql("DELETE FROM permissions").update();

                jdbcClient.sql("DELETE FROM employee_access_grants").update();
                jdbcClient.sql("DELETE FROM employees").update();
                jdbcClient.sql("DELETE FROM warehouses").update();
                jdbcClient.sql("DELETE FROM registration_codes").update();
                jdbcClient.sql("DELETE FROM devices").update();
                jdbcClient.sql("DELETE FROM store_settings").update();
                jdbcClient.sql("DELETE FROM stores").update();
                jdbcClient.sql("DELETE FROM companies").update();
                jdbcClient.sql("DELETE FROM organizations").update();
        }

        protected void cleanTransactionalTables() {
                // STRICT DELETION ORDER TO PREVENT FOREIGN KEY VIOLATIONS
                jdbcClient.sql("DELETE FROM email_delivery_events").update();
                jdbcClient.sql("DELETE FROM marketing_email_logs").update();
                jdbcClient.sql("DELETE FROM marketing_campaigns").update();
                jdbcClient.sql("DELETE FROM marketing_segments").update();
                jdbcClient.sql("DELETE FROM marketing_templates").update();
                jdbcClient.sql("DELETE FROM company_assets").update();
                jdbcClient.sql("DELETE FROM company_marketing_settings").update();
                jdbcClient.sql("DELETE FROM customers").update();
        }

        protected HttpHeaders loginAndGetHeaders(String username, String password) {
                LoginRequest loginRequest = new LoginRequest(username, password);
                ResponseEntity<ApiResponse<LoginResponse>> response = restTemplate.exchange("/api/v1/auth/login",
                                HttpMethod.POST, new HttpEntity<>(loginRequest),
                                new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {
                                });

                if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null)
                        throw new RuntimeException("Login failed for user: " + username);

                String fullCookieValue = response.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                                .filter(c -> c.startsWith("ACCESS_TOKEN=")).findFirst()
                                .orElseThrow(() -> new RuntimeException("ACCESS_TOKEN cookie not found"));

                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.COOKIE, fullCookieValue.split(";")[0]);

                if (response.getBody().getData() != null && response.getBody().getData().token() != null) {
                        globalAdminToken = response.getBody().getData().token();
                        headers.setBearerAuth(globalAdminToken);
                }

                return headers;
        }

        protected UUID createTestCustomer(String legalName, String taxId) {
                UUID uuid = UUID.randomUUID();
                jdbcClient.sql(
                                "INSERT INTO customers (uuid, company_id, tax_id, legal_name, commercial_name, country, allow_credit, credit_limit, surcharge_apply, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 'ES', FALSE, 0, FALSE, TRUE, NOW(), NOW())")
                                .param(uuid).param(globalCompanyId).param(taxId).param(legalName).param(legalName)
                                .update();
                return uuid;
        }
}