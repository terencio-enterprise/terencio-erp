package es.terencio.erp.organization.infrastructure.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.terencio.erp.AbstractIntegrationTest;
import es.terencio.erp.shared.presentation.ApiResponse;

class OrganizationIntegrationTest extends AbstractIntegrationTest {

        @Test
        void shouldCreateCompany() {
                Map<String, Object> createRequest = Map.of("organizationId", globalOrgId.toString(), "name",
                                "Test Company SA", "taxId", "B12345678", "currencyCode", "EUR", "fiscalRegime",
                                "COMMON",
                                "priceIncludesTax", true, "roundingMode", "LINE");

                ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange("/api/v1/companies",
                                HttpMethod.POST, new HttpEntity<>(createRequest, globalAdminHeaders),
                                new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                Map<String, Object> company = java.util.Objects.requireNonNull(response.getBody()).getData();
                assertThat(company.get("name")).isEqualTo("Test Company SA");
                UUID companyId = UUID.fromString((String) company.get("companyId"));

                Integer count = jdbcClient.sql("SELECT COUNT(*) FROM companies WHERE id = ?").param(companyId)
                                .query(Integer.class).single();
                assertThat(count).isEqualTo(1);
        }

        @Test
        void shouldUpdateFiscalSettings() {
                Map<String, Object> updateRequest = Map.of("fiscalRegime", "SII", "priceIncludesTax", false,
                                "roundingMode",
                                "TOTAL");
                ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange(
                                "/api/v1/companies/" + globalCompanyId + "/fiscal-settings", HttpMethod.PUT,
                                new HttpEntity<>(updateRequest, globalAdminHeaders),
                                new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(java.util.Objects.requireNonNull(response.getBody()).getData().get("fiscalRegime"))
                                .isEqualTo("SII");
        }

        @Test
        void shouldCreateStoreWithAutoWarehouse() {
                Map<String, Object> createRequest = Map.of("companyId", globalCompanyId.toString(), "code", "STORE001",
                                "name",
                                "Main Store", "street", "Test Street 123", "zipCode", "28001", "city", "Madrid");

                ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange("/api/v1/stores",
                                HttpMethod.POST, new HttpEntity<>(createRequest, globalAdminHeaders),
                                new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                Map<String, Object> store = java.util.Objects.requireNonNull(response.getBody()).getData();
                UUID warehouseId = UUID.fromString((String) store.get("warehouseId"));

                Integer warehouseCount = jdbcClient.sql("SELECT COUNT(*) FROM warehouses WHERE id = ?")
                                .param(warehouseId)
                                .query(Integer.class).single();
                assertThat(warehouseCount).isEqualTo(1);
        }
}