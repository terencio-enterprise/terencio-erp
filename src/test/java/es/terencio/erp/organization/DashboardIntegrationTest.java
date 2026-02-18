package es.terencio.erp.organization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import es.terencio.erp.employees.application.port.out.EmployeePort;
import es.terencio.erp.organization.application.dto.DashboardContextDto;
import es.terencio.erp.organization.presentation.OrganizationController.SwitchContextRequest;
import es.terencio.erp.shared.presentation.ApiResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DashboardIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmployeePort employeePort;

    private String token;
    private Long employeeId;
    private UUID orgId;
    private UUID companyId;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        // Clean DB
        jdbcClient.sql("DELETE FROM employee_access_grants").update();
        jdbcClient.sql("DELETE FROM employees").update();
        jdbcClient.sql("DELETE FROM stores").update();
        jdbcClient.sql("DELETE FROM companies").update();
        jdbcClient.sql("DELETE FROM organizations").update();

        // 1. Create Org Structure
        orgId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        storeId = UUID.randomUUID();

        jdbcClient.sql("INSERT INTO organizations (id, name, slug) VALUES (?, ?, ?)")
                .params(orgId, "Test Org", "test-org").update();

        jdbcClient.sql("INSERT INTO companies (id, name, slug, organization_id) VALUES (?, ?, ?, ?)")
                .params(companyId, "Test Company", "test-company", orgId).update();

        jdbcClient.sql("INSERT INTO stores (id, name, slug, code, company_id) VALUES (?, ?, ?, ?, ?)")
                .params(storeId, "Test Store", "test-store", "TS001", companyId).update();

        // 2. Create Employee
        String passwordHash = passwordEncoder.encode("password");
        employeeId = System.currentTimeMillis(); // Simple ID generation

        jdbcClient.sql("""
                INSERT INTO employees (id, username, password_hash, full_name, role, is_active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)
                .params(employeeId, "testuser", passwordHash, "Test User", "USER", true, Instant.now(), Instant.now())
                .update();

        // 3. Grant Access (Store Level)
        jdbcClient.sql("INSERT INTO employee_access_grants (employee_id, scope, target_id, role) VALUES (?, ?, ?, ?)")
                .params(employeeId, "STORE", storeId, "MANAGER").update();

        // 4. Login to get token
        var loginRequest = new es.terencio.erp.auth.application.dto.LoginRequest("testuser", "password");
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/login", loginRequest, String.class);

        // Extract token from response (Assuming JSON response structure from
        // AuthController)
        // For simplicity in test, we might fallback to cookie if implemented,
        // but let's assume body contains token or we can parse it.
        // Actually, previous tests used helper or parsed body.
        // Let's assume standard token response.
        // If AuthController returns ApiResponse<TokenDto>, we parse that.
        // But wait, the user implemented Cookie based auth in previous turn?
        // "Access Token Cookie Refactor" - YES.
        // So we need to grab the cookie "accessToken".

        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies != null) {
            token = cookies.stream()
                    .filter(c -> c.startsWith("accessToken="))
                    .findFirst()
                    .map(c -> c.substring("accessToken=".length()).split(";")[0])
                    .orElse(null);
        }
    }

    @Test
    void testDashboardContextFlow() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "accessToken=" + token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // 1. Get Context via /me (Should default to the only available store)
        ResponseEntity<ApiResponse<es.terencio.erp.auth.application.dto.EmployeeInfoDto>> response = restTemplate
                .exchange(
                        "/api/v1/auth/me",
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<ApiResponse<es.terencio.erp.auth.application.dto.EmployeeInfoDto>>() {
                        });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DashboardContextDto context = response.getBody().getData().context();
        assertThat(context.activeStore().id()).isEqualTo(storeId);
        assertThat(context.activeCompany().id()).isEqualTo(companyId); // Should derive company from store
        assertThat(context.availableStores()).hasSize(1);

        // 2. Switch Context (Same context, should succeed and update DB)
        SwitchContextRequest switchRequest = new SwitchContextRequest(companyId, storeId);
        HttpEntity<SwitchContextRequest> switchEntity = new HttpEntity<>(switchRequest, headers);

        ResponseEntity<ApiResponse<Void>> switchResponse = restTemplate.exchange(
                "/api/v1/organizations/context",
                HttpMethod.PUT,
                switchEntity,
                new ParameterizedTypeReference<ApiResponse<Void>>() {
                });

        assertThat(switchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify Persistence
        var employee = employeePort.findById(employeeId).orElseThrow();
        assertThat(employee.lastActiveStoreId()).isEqualTo(storeId);
        assertThat(employee.lastActiveCompanyId()).isEqualTo(companyId);
    }
}
