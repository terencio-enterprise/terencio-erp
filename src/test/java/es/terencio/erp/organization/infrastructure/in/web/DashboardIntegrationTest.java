package es.terencio.erp.organization.infrastructure.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.terencio.erp.AbstractIntegrationTest;
import es.terencio.erp.auth.application.dto.AuthDtos.EmployeeInfoDto;
import es.terencio.erp.organization.infrastructure.in.web.OrganizationController.SwitchContextRequest;
import es.terencio.erp.shared.presentation.ApiResponse;

class DashboardIntegrationTest extends AbstractIntegrationTest {

    @Test
    void testDashboardContextFlow() {
        HttpEntity<Void> entity = new HttpEntity<>(globalAdminHeaders);

        ResponseEntity<ApiResponse<EmployeeInfoDto>> response = restTemplate.exchange("/api/v1/auth/me", HttpMethod.GET,
                entity, new ParameterizedTypeReference<ApiResponse<EmployeeInfoDto>>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var companies = response.getBody().getData().companies();
        assertThat(companies).isNotEmpty();
        assertThat(companies.get(0).id()).isEqualTo(globalCompanyId);
        assertThat(companies.get(0).stores().get(0).id()).isEqualTo(globalStoreId);

        SwitchContextRequest switchRequest = new SwitchContextRequest(globalCompanyId, globalStoreId);
        HttpEntity<SwitchContextRequest> switchEntity = new HttpEntity<>(switchRequest, globalAdminHeaders);

        ResponseEntity<ApiResponse<Void>> switchResponse = restTemplate.exchange("/api/v1/organizations/context",
                HttpMethod.PUT, switchEntity, new ParameterizedTypeReference<ApiResponse<Void>>() {
                });
        assertThat(switchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Fetch using raw JDBC to guarantee cache bypassed assertion directly in the DB
        UUID activeCompanyId = jdbcClient.sql("SELECT last_active_company_id FROM employees WHERE id = :id")
                .param("id", globalAdminId).query(UUID.class).single();
        UUID activeStoreId = jdbcClient.sql("SELECT last_active_store_id FROM employees WHERE id = :id")
                .param("id", globalAdminId).query(UUID.class).single();

        assertThat(activeCompanyId).isEqualTo(globalCompanyId);
        assertThat(activeStoreId).isEqualTo(globalStoreId);
    }
}