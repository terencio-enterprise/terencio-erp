package es.terencio.erp.auth.infrastructure.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import es.terencio.erp.AbstractIntegrationTest;
import es.terencio.erp.auth.application.dto.AuthDtos.EmployeeInfoDto;
import es.terencio.erp.auth.application.dto.AuthDtos.LoginRequest;
import es.terencio.erp.auth.application.dto.AuthDtos.LoginResponse;
import es.terencio.erp.shared.presentation.ApiResponse;

class AuthenticationIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcClient jdbcClient;
    @Autowired private PasswordEncoder passwordEncoder;

    private UUID testCompanyId, testOrganizationId, testStoreId;
    private Long adminEmployeeId, cashierEmployeeId, managerEmployeeId;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        testOrganizationId = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO organizations (id, name, slug, subscription_plan) VALUES (:id, 'Test Org', 'test-org', 'STANDARD')").param("id", testOrganizationId).update();
        
        testCompanyId = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO companies (id, organization_id, name, slug, tax_id, is_active) VALUES (:id, :org, 'Test Comp', 'test-comp', 'B11111111', TRUE)")
            .param("id", testCompanyId).param("org", testOrganizationId).update();
            
        testStoreId = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO stores (id, company_id, code, name, slug, address, is_active) VALUES (:id, :comp, 'TEST', 'Store', 'test', 'Add', TRUE)")
            .param("id", testStoreId).param("comp", testCompanyId).update();

        String pwd = passwordEncoder.encode("admin123");
        adminEmployeeId = jdbcClient.sql("INSERT INTO employees (username, full_name, role, pin_hash, password_hash, organization_id, store_id, permissions_json, is_active) VALUES ('admin', 'Admin', 'ADMIN', 'pin', :pwd, :org, :store, '[]', TRUE) RETURNING id")
            .param("pwd", pwd).param("org", testOrganizationId).param("store", testStoreId).query(Long.class).single();
    }

    @Test
    void testSuccessfulLogin() {
        ResponseEntity<ApiResponse<LoginResponse>> response = restTemplate.exchange("/api/v1/auth/login", HttpMethod.POST, new HttpEntity<>(new LoginRequest("admin", "admin123")), new ParameterizedTypeReference<>() {});
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().token()).isNotBlank();
    }
}
