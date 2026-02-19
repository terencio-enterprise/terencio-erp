package es.terencio.erp.organization.infrastructure.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import es.terencio.erp.AbstractIntegrationTest;
import es.terencio.erp.auth.application.dto.AuthDtos.EmployeeInfoDto;
import es.terencio.erp.employees.application.port.out.EmployeePort;
import es.terencio.erp.organization.infrastructure.in.web.OrganizationController.SwitchContextRequest;
import es.terencio.erp.shared.presentation.ApiResponse;

class DashboardIntegrationTest extends AbstractIntegrationTest {

    @Autowired private EmployeePort employeePort;

    private Long employeeId;
    private UUID orgId, companyId, storeId;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        orgId = UUID.randomUUID(); companyId = UUID.randomUUID(); storeId = UUID.randomUUID();

        jdbcClient.sql("INSERT INTO organizations (id, name, slug, subscription_plan) VALUES (?, ?, ?, ?)").params(orgId, "Test Org", "test-org", "STANDARD").update();
        jdbcClient.sql("INSERT INTO companies (id, name, slug, organization_id, tax_id, is_active) VALUES (?, ?, ?, ?, ?, ?)").params(companyId, "Test Company", "test-company", orgId, "B11111111", true).update();
        jdbcClient.sql("INSERT INTO stores (id, name, slug, code, company_id) VALUES (?, ?, ?, ?, ?)").params(storeId, "Test Store", "test-store", "TS001", companyId).update();

        String passwordHash = passwordEncoder.encode("password");
        employeeId = jdbcClient.sql("INSERT INTO employees (username, password_hash, full_name, role, is_active, created_at, updated_at) VALUES ('testuser', ?, 'Test User', 'USER', true, NOW(), NOW()) RETURNING id").params(passwordHash).query(Long.class).single();
        jdbcClient.sql("INSERT INTO employee_access_grants (employee_id, scope, target_id, role) VALUES (?, ?, ?, ?)").params(employeeId, "STORE", storeId, "MANAGER").update();
    }

    @Test
    void testDashboardContextFlow() {
        HttpHeaders headers = loginAndGetHeaders("testuser", "password");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<EmployeeInfoDto>> response = restTemplate.exchange("/api/v1/auth/me", HttpMethod.GET, entity, new ParameterizedTypeReference<ApiResponse<EmployeeInfoDto>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var companies = response.getBody().getData().companies();
        assertThat(companies).isNotEmpty();
        assertThat(companies.get(0).id()).isEqualTo(companyId);
        assertThat(companies.get(0).stores().get(0).id()).isEqualTo(storeId);

        SwitchContextRequest switchRequest = new SwitchContextRequest(companyId, storeId);
        HttpEntity<SwitchContextRequest> switchEntity = new HttpEntity<>(switchRequest, headers);

        ResponseEntity<ApiResponse<Void>> switchResponse = restTemplate.exchange("/api/v1/organizations/context", HttpMethod.PUT, switchEntity, new ParameterizedTypeReference<ApiResponse<Void>>() {});
        assertThat(switchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        var employee = employeePort.findById(employeeId).orElseThrow();
        assertThat(employee.lastActiveStoreId()).isEqualTo(storeId);
        assertThat(employee.lastActiveCompanyId()).isEqualTo(companyId);
    }
}
