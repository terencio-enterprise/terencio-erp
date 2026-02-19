package es.terencio.erp.organization.infrastructure.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import es.terencio.erp.AbstractIntegrationTest;
import es.terencio.erp.shared.presentation.ApiResponse;

class OrganizationIntegrationTest extends AbstractIntegrationTest {

    @Autowired private JdbcClient jdbcClient;
    private UUID testCompanyId;
    private org.springframework.http.HttpHeaders authHeaders;

    @BeforeEach
    void setUp() { cleanDatabase(); }

    @Test
    void shouldCreateCompany() {
        Map<String, Object> createRequest = Map.of("organizationId", UUID.randomUUID().toString(), "name", "Test Company SA", "taxId", "B12345678", "currencyCode", "EUR", "fiscalRegime", "COMMON", "priceIncludesTax", true, "roundingMode", "LINE");
        UUID bootstrapCompanyId = createTestCompany();
        UUID bootstrapStoreId = createStore(bootstrapCompanyId);
        createAdminUser(bootstrapCompanyId, bootstrapStoreId, "bootstrap_admin", "admin123");
        org.springframework.http.HttpHeaders bootstrapHeaders = loginAndGetHeaders("bootstrap_admin", "admin123");

        ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange("/api/v1/companies", HttpMethod.POST, new HttpEntity<>(createRequest, bootstrapHeaders), new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> company = java.util.Objects.requireNonNull(response.getBody()).getData();
        assertThat(company.get("name")).isEqualTo("Test Company SA");
        UUID companyId = UUID.fromString((String) company.get("companyId"));
        Integer count = jdbcClient.sql("SELECT COUNT(*) FROM companies WHERE id = ?").param(companyId).query(Integer.class).single();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldUpdateFiscalSettings() {
        UUID companyId = createTestCompany();
        UUID storeId = createStore(companyId);
        createAdminUser(companyId, storeId, "admin", "admin123");
        authHeaders = loginAndGetHeaders("admin", "admin123");

        Map<String, Object> updateRequest = Map.of("fiscalRegime", "SII", "priceIncludesTax", false, "roundingMode", "TOTAL");
        ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange("/api/v1/companies/" + companyId + "/fiscal-settings", HttpMethod.PUT, new HttpEntity<>(updateRequest, authHeaders), new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {});
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(java.util.Objects.requireNonNull(response.getBody()).getData().get("fiscalRegime")).isEqualTo("SII");
    }

    @Test
    void shouldCreateStoreWithAutoWarehouse() {
        UUID companyId = createTestCompany();
        UUID authStoreId = createStore(companyId);
        createAdminUser(companyId, authStoreId, "admin", "admin123");
        authHeaders = loginAndGetHeaders("admin", "admin123");

        Map<String, Object> createRequest = Map.of("companyId", companyId.toString(), "code", "STORE001", "name", "Main Store", "street", "Test Street 123", "zipCode", "28001", "city", "Madrid");
        ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange("/api/v1/stores", HttpMethod.POST, new HttpEntity<>(createRequest, authHeaders), new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> store = java.util.Objects.requireNonNull(response.getBody()).getData();
        UUID warehouseId = UUID.fromString((String) store.get("warehouseId"));

        Integer warehouseCount = jdbcClient.sql("SELECT COUNT(*) FROM warehouses WHERE id = ?").param(warehouseId).query(Integer.class).single();
        assertThat(warehouseCount).isEqualTo(1);
    }

    private UUID createTestCompany() {
        testCompanyId = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO companies (id, name, tax_id, currency_code, fiscal_regime, price_includes_tax, rounding_mode, is_active, created_at, updated_at, version) VALUES (?, 'Test Company', 'B11111111', 'EUR', 'COMMON', TRUE, 'LINE', TRUE, NOW(), NOW(), 1)").param(testCompanyId).update();
        return testCompanyId;
    }
}
