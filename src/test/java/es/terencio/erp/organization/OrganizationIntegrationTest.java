package es.terencio.erp.organization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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

/**
 * Integration tests for Organization module (Company, Store, StoreSettings).
 * Tests all REST endpoints and verifies database persistence and business
 * rules.
 */
class OrganizationIntegrationTest extends AbstractIntegrationTest {

        @Autowired
        private JdbcClient jdbcClient;

        private UUID testCompanyId;
        private org.springframework.http.HttpHeaders authHeaders;

        @BeforeEach
        void setUp() {
                cleanDatabase();
        }

        @Test
        void shouldCreateCompany() {
                // Given
                Map<String, Object> createRequest = Map.of(
                                "name", "Test Company SA",
                                "taxId", "B12345678",
                                "currencyCode", "EUR",
                                "fiscalRegime", "COMMON",
                                "priceIncludesTax", true,
                                "roundingMode", "LINE");

                // When
                // Special case: Creating a company might be public or require super-admin.
                // Assuming public for initial setup or self-registration, OR we need a
                // pre-existing user if it's protected.
                // If protected, we need to bootstrap an admin first.
                // Let's assume for now we use the auth headers if we have them, BUT for company
                // creation usually it's open or super-admin.
                // However, if we don't have a company, we don't have a user.
                // Let's bootstrap a dummy company/user just to get a token if needed, or check
                // if this endpoint is public.
                // IF endpoint is public:
                ResponseEntity<ApiResponse<Map>> response = restTemplate.exchange(
                                "/api/v1/companies",
                                HttpMethod.POST,
                                new HttpEntity<>(createRequest),
                                new ParameterizedTypeReference<ApiResponse<Map>>() {
                                });

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isTrue();
                Map company = response.getBody().getData();
                assertThat(company.get("name")).isEqualTo("Test Company SA");
                assertThat(company.get("taxId")).isEqualTo("B12345678");

                UUID companyId = UUID.fromString((String) company.get("companyId"));

                // Verify persistence
                Integer count = jdbcClient.sql("SELECT COUNT(*) FROM companies WHERE id = ?")
                                .param(companyId)
                                .query(Integer.class)
                                .single();
                assertThat(count).isEqualTo(1);
        }

        @Test
        void shouldGetCompanyById() {
                // Given
                UUID companyId = createTestCompany();
                // Setup auth for this company
                UUID storeId = createStore(companyId);
                createAdminUser(companyId, storeId, "admin", "admin123");
                authHeaders = loginAndGetHeaders("admin", "admin123");

                // When
                ResponseEntity<ApiResponse<Map>> response = restTemplate.exchange(
                                "/api/v1/companies/" + companyId,
                                HttpMethod.GET,
                                new HttpEntity<>(authHeaders),
                                new ParameterizedTypeReference<ApiResponse<Map>>() {
                                });

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isTrue();
                Map company = response.getBody().getData();
                assertThat(company.get("id")).isEqualTo(companyId.toString());
                assertThat(company.get("fiscalRegime")).isEqualTo("COMMON");
        }

        @Test
        void shouldUpdateFiscalSettings() {
                // Given
                UUID companyId = createTestCompany();
                // Setup auth for this company
                UUID storeId = createStore(companyId);
                createAdminUser(companyId, storeId, "admin", "admin123");
                authHeaders = loginAndGetHeaders("admin", "admin123");

                Map<String, Object> updateRequest = Map.of(
                                "fiscalRegime", "SII",
                                "priceIncludesTax", false,
                                "roundingMode", "TOTAL");

                // When
                ResponseEntity<ApiResponse<Map>> response = restTemplate.exchange(
                                "/api/v1/companies/" + companyId + "/fiscal-settings",
                                HttpMethod.PUT,
                                new HttpEntity<>(updateRequest, authHeaders),
                                new ParameterizedTypeReference<ApiResponse<Map>>() {
                                });

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isTrue();
                Map company = response.getBody().getData();
                assertThat(company.get("fiscalRegime")).isEqualTo("SII");
                assertThat(company.get("roundingMode")).isEqualTo("TOTAL");

                // Verify persistence
                String fiscalRegime = jdbcClient.sql("SELECT fiscal_regime FROM companies WHERE id = ?")
                                .param(companyId)
                                .query(String.class)
                                .single();
                assertThat(fiscalRegime).isEqualTo("SII");
        }

        @Test
        void shouldCreateStoreWithAutoWarehouse() {
                // Given
                UUID companyId = createTestCompany();
                // Setup auth
                // Setup auth
                UUID authStoreId = createStore(companyId);
                createAdminUser(companyId, authStoreId, "admin", "admin123");
                authHeaders = loginAndGetHeaders("admin", "admin123");

                Map<String, Object> createRequest = Map.of(
                                "companyId", companyId.toString(),
                                "code", "STORE001",
                                "name", "Main Store",
                                "street", "Test Street 123",
                                "zipCode", "28001",
                                "city", "Madrid");

                // When
                ResponseEntity<ApiResponse<Map>> response = restTemplate.exchange(
                                "/api/v1/stores",
                                HttpMethod.POST,
                                new HttpEntity<>(createRequest, authHeaders),
                                new ParameterizedTypeReference<ApiResponse<Map>>() {
                                });

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isTrue();
                Map store = response.getBody().getData();
                assertThat(store.get("storeCode")).isEqualTo("STORE001");

                UUID storeId = UUID.fromString((String) store.get("storeId"));
                UUID warehouseId = UUID.fromString((String) store.get("warehouseId"));

                // Verify store persistence
                Integer storeCount = jdbcClient.sql("SELECT COUNT(*) FROM stores WHERE id = ?")
                                .param(storeId)
                                .query(Integer.class)
                                .single();
                assertThat(storeCount).isEqualTo(1);

                // Verify warehouse auto-creation
                Integer warehouseCount = jdbcClient.sql("SELECT COUNT(*) FROM warehouses WHERE id = ?")
                                .param(warehouseId)
                                .query(Integer.class)
                                .single();
                assertThat(warehouseCount).isEqualTo(1);

                // Verify store settings auto-creation
                Integer settingsCount = jdbcClient.sql("SELECT COUNT(*) FROM store_settings WHERE store_id = ?")
                                .param(storeId)
                                .query(Integer.class)
                                .single();
                assertThat(settingsCount).isEqualTo(1);
        }

        @Test
        void shouldListStores() {
                // Given
                UUID companyId = createTestCompany();
                createTestStore(companyId, "STORE001", "Store One");
                createTestStore(companyId, "STORE002", "Store Two ");

                // Setup auth
                UUID storeId = createStore(companyId);
                createAdminUser(companyId, storeId, "admin", "admin123");
                authHeaders = loginAndGetHeaders("admin", "admin123");

                // When
                ResponseEntity<ApiResponse<List<Map>>> response = restTemplate.exchange(
                                "/api/v1/stores?companyId=" + companyId,
                                HttpMethod.GET,
                                new HttpEntity<>(authHeaders),
                                new ParameterizedTypeReference<ApiResponse<List<Map>>>() {
                                });

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isTrue();
                List<Map> stores = response.getBody().getData();
                assertThat(stores).hasSize(2);
        }

        @Test
        void shouldGetStoreSettings() {
                // Given
                UUID companyId = createTestCompany();
                UUID storeId = createTestStore(companyId, "STORE001", "Test Store");

                // Setup auth
                createAdminUser(companyId, storeId, "admin", "admin123");
                authHeaders = loginAndGetHeaders("admin", "admin123");

                // When
                ResponseEntity<ApiResponse<Map>> response = restTemplate.exchange(
                                "/api/v1/stores/" + storeId + "/settings",
                                HttpMethod.GET,
                                new HttpEntity<>(authHeaders),
                                new ParameterizedTypeReference<ApiResponse<Map>>() {
                                });

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isTrue();
                Map settings = response.getBody().getData();
                assertThat(settings.get("allowNegativeStock")).isEqualTo(false);
                assertThat(settings.get("printTicketAutomatically")).isEqualTo(true);
        }

        @Test
        void shouldUpdateStoreSettings() {
                // Given
                UUID companyId = createTestCompany();
                UUID storeId = createTestStore(companyId, "STORE001", "Test Store");

                // Setup auth
                createAdminUser(companyId, storeId, "admin", "admin123");
                authHeaders = loginAndGetHeaders("admin", "admin123");

                Map<String, Object> updateRequest = Map.of(
                                "storeId", storeId.toString(),
                                "allowNegativeStock", true,
                                "printTicketAutomatically", false,
                                "requireCustomerForLargeAmountCents", 150000L);

                // When
                ResponseEntity<Void> response = restTemplate.exchange(
                                "/api/v1/stores/" + storeId + "/settings",
                                HttpMethod.PUT,
                                new HttpEntity<>(updateRequest, authHeaders),
                                Void.class);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

                // Verify persistence
                Boolean allowNegativeStock = jdbcClient.sql(
                                "SELECT allow_negative_stock FROM store_settings WHERE store_id = ?")
                                .param(storeId)
                                .query(Boolean.class)
                                .single();
                assertThat(allowNegativeStock).isTrue();
        }

        // ==================== HELPER METHODS ====================

        private UUID createTestCompany() {
                testCompanyId = UUID.randomUUID();
                jdbcClient.sql("""
                                INSERT INTO companies (id, name, tax_id, currency_code, fiscal_regime,
                                    price_includes_tax, rounding_mode, is_active, created_at, updated_at, version)
                                VALUES (?, 'Test Company', 'B11111111', 'EUR', 'COMMON', TRUE, 'LINE', TRUE, NOW(), NOW(), 1)
                                """)
                                .param(testCompanyId)
                                .update();
                return testCompanyId;
        }

        private UUID createTestStore(UUID companyId, String code, String name) {
                UUID storeId = UUID.randomUUID();
                jdbcClient.sql("""
                                INSERT INTO stores (id, company_id, code, name, is_active, timezone, created_at, updated_at, version)
                                VALUES (?, ?, ?, ?, TRUE, 'Europe/Madrid', NOW(), NOW(), 1)
                                """)
                                .param(storeId)
                                .param(companyId)
                                .param(code)
                                .param(name)
                                .update();

                // Create warehouse
                UUID warehouseId = UUID.randomUUID();
                jdbcClient.sql("""
                                INSERT INTO warehouses (id, store_id, name, code, created_at)
                                VALUES (?, ?, ?, ?, NOW())
                                """)
                                .param(warehouseId)
                                .param(storeId)
                                .param("Warehouse " + name)
                                .param("WH-" + code)
                                .update();

                // Create settings
                jdbcClient.sql("""
                                INSERT INTO store_settings (store_id, allow_negative_stock, print_ticket_automatically,
                                    require_customer_for_large_amount, updated_at)
                                VALUES (?, FALSE, TRUE, 100000, NOW())
                                """)
                                .param(storeId)
                                .update();

                return storeId;
        }
}
