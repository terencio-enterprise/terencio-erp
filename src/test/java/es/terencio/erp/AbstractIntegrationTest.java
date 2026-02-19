package es.terencio.erp;

import java.util.UUID;

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

    protected void cleanDatabase() {
        jdbcClient.sql("DELETE FROM audit_user_actions").update();
        jdbcClient.sql("DELETE FROM marketing_logs").update();
        jdbcClient.sql("DELETE FROM marketing_templates").update();
        jdbcClient.sql("DELETE FROM accounting_entry_lines").update();
        jdbcClient.sql("DELETE FROM fiscal_audit_log").update();
        jdbcClient.sql("DELETE FROM payments").update();
        jdbcClient.sql("DELETE FROM sale_taxes").update();
        jdbcClient.sql("DELETE FROM sale_lines").update();
        jdbcClient.sql("DELETE FROM cash_movements").update();
        jdbcClient.sql("DELETE FROM customer_account_movements").update();
        jdbcClient.sql("DELETE FROM stock_movements").update();
        jdbcClient.sql("DELETE FROM inventory_stock").update();
        jdbcClient.sql("DELETE FROM customer_product_prices").update();
        jdbcClient.sql("DELETE FROM product_prices").update();
        jdbcClient.sql("DELETE FROM product_barcodes").update();

        jdbcClient.sql("DELETE FROM sales").update();
        jdbcClient.sql("DELETE FROM accounting_entries").update();
        jdbcClient.sql("DELETE FROM products").update();
        jdbcClient.sql("DELETE FROM customers").update();

        jdbcClient.sql("DELETE FROM shifts").update();
        jdbcClient.sql("DELETE FROM registration_codes").update();
        jdbcClient.sql("DELETE FROM devices").update();
        jdbcClient.sql("DELETE FROM employee_access_grants").update();
        jdbcClient.sql("DELETE FROM employees").update();
        jdbcClient.sql("DELETE FROM warehouses").update();
        jdbcClient.sql("DELETE FROM store_settings").update();
        jdbcClient.sql("DELETE FROM stores").update();

        jdbcClient.sql("DELETE FROM pricing_rules").update();
        jdbcClient.sql("DELETE FROM categories").update();
        jdbcClient.sql("DELETE FROM taxes").update();
        jdbcClient.sql("DELETE FROM tariffs").update();
        jdbcClient.sql("DELETE FROM payment_methods").update();

        jdbcClient.sql("DELETE FROM companies").update();
        jdbcClient.sql("DELETE FROM organizations").update();
    }

    protected UUID createTestCompany() {
        UUID orgId = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO organizations (id, name, slug) VALUES (:id, 'Test Org', :slug)")
                .param("id", orgId).param("slug", "test-org-" + orgId.toString().substring(0, 8)).update();
        UUID companyId = UUID.randomUUID();
        jdbcClient.sql(
                "INSERT INTO companies (id, organization_id, name, slug, tax_id, currency_code, fiscal_regime, price_includes_tax, rounding_mode, is_active, created_at, updated_at, version) VALUES (:id, :orgId, 'Test Company', :slug, 'B11111111', 'EUR', 'COMMON', TRUE, 'LINE', TRUE, NOW(), NOW(), 1)")
                .param("id", companyId).param("orgId", orgId)
                .param("slug", "test-co-" + companyId.toString().substring(0, 8)).update();
        return companyId;
    }

    protected UUID createStore(UUID companyId) {
        UUID storeId = UUID.randomUUID();
        jdbcClient.sql(
                "INSERT INTO stores (id, company_id, code, name, slug, address, is_active) VALUES (:id, :companyId, 'TEST-STORE', 'Test Store', :slug, 'Test Address', TRUE)")
                .param("id", storeId).param("companyId", companyId)
                .param("slug", "test-store-" + storeId.toString().substring(0, 8)).update();
        return storeId;
    }

    protected void createAdminUser(UUID companyId, UUID storeId, String username, String password) {
        String encodedPassword = passwordEncoder.encode(password);
        Long employeeId = jdbcClient.sql(
                "INSERT INTO employees (username, full_name, pin_hash, password_hash, organization_id, is_active, created_at, updated_at) VALUES (:username, 'Admin User', 'pin123', :password, (SELECT organization_id FROM companies WHERE id = :companyId), TRUE, NOW(), NOW()) RETURNING id")
                .param("username", username).param("password", encodedPassword).param("companyId", companyId)
                .query(Long.class).single();

        // Grant company-level access (needed for COMPANY-scope permission checks)
        jdbcClient.sql(
                "INSERT INTO employee_access_grants (employee_id, scope, target_id, role, created_at) VALUES (:employeeId, 'COMPANY', :targetId, 'ADMIN', NOW())")
                .param("employeeId", employeeId).param("targetId", companyId).update();

        if (storeId != null) {
            jdbcClient.sql(
                    "INSERT INTO employee_access_grants (employee_id, scope, target_id, role, created_at) VALUES (:employeeId, 'STORE', :targetId, 'ADMIN', NOW())")
                    .param("employeeId", employeeId).param("targetId", storeId).update();
        }

        // Ensure CRM permissions exist and are assigned to ADMIN role
        jdbcClient.sql(
                "INSERT INTO permissions (code, name, description, module) VALUES ('customer:view', 'View Customers', 'View customers', 'CRM') ON CONFLICT (code) DO NOTHING")
                .update();
        jdbcClient.sql(
                "INSERT INTO permissions (code, name, description, module) VALUES ('customer:create', 'Create Customer', 'Create customers', 'CRM') ON CONFLICT (code) DO NOTHING")
                .update();
        jdbcClient.sql(
                "INSERT INTO permissions (code, name, description, module) VALUES ('customer:update', 'Edit Customer', 'Edit customers', 'CRM') ON CONFLICT (code) DO NOTHING")
                .update();
        jdbcClient.sql(
                "INSERT INTO role_permissions (role_name, permission_code) VALUES ('ADMIN', 'customer:view') ON CONFLICT DO NOTHING")
                .update();
        jdbcClient.sql(
                "INSERT INTO role_permissions (role_name, permission_code) VALUES ('ADMIN', 'customer:create') ON CONFLICT DO NOTHING")
                .update();
        jdbcClient.sql(
                "INSERT INTO role_permissions (role_name, permission_code) VALUES ('ADMIN', 'customer:update') ON CONFLICT DO NOTHING")
                .update();
    }

    protected HttpHeaders createAuthenticatedUser(String username, String role, String scope, UUID targetId,
            String... permissionCodes) {
        String encodedPassword = passwordEncoder.encode("test123");
        Long employeeId = jdbcClient.sql(
                "INSERT INTO employees (username, full_name, pin_hash, password_hash, organization_id, is_active, created_at, updated_at) VALUES (:username, :username, 'pin000', :password, (SELECT organization_id FROM companies WHERE id = :targetId), TRUE, NOW(), NOW()) RETURNING id")
                .param("username", username).param("password", encodedPassword)
                .param("targetId", targetId).query(Long.class).single();

        jdbcClient.sql(
                "INSERT INTO employee_access_grants (employee_id, scope, target_id, role, created_at) VALUES (:employeeId, :scope, :targetId, :role, NOW())")
                .param("employeeId", employeeId).param("scope", scope).param("targetId", targetId).param("role", role)
                .update();

        for (String code : permissionCodes) {
            jdbcClient.sql(
                    "INSERT INTO role_permissions (role_name, permission_code) VALUES (:role, :code) ON CONFLICT DO NOTHING")
                    .param("role", role).param("code", code).update();
        }
        return loginAndGetHeaders(username, "test123");
    }

    protected HttpHeaders loginAndGetHeaders(String username, String password) {
        LoginRequest loginRequest = new LoginRequest(username, password);
        ResponseEntity<ApiResponse<LoginResponse>> response = restTemplate.exchange("/api/v1/auth/login",
                HttpMethod.POST, new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {
                });

        if (response.getStatusCode() != HttpStatus.OK)
            throw new RuntimeException("Login failed for user: " + username);

        String accessTokenCookie = response.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith("ACCESS_TOKEN=")).findFirst()
                .orElseThrow(() -> new RuntimeException("ACCESS_TOKEN cookie not found"));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, accessTokenCookie);
        return headers;
    }
}
