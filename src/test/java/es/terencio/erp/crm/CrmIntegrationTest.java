package es.terencio.erp.crm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;

import es.terencio.erp.AbstractIntegrationTest;
import es.terencio.erp.shared.presentation.ApiResponse;

/**
 * Integration tests for CRM module (Customers, Commercial terms, Special
 * prices).
 */
class CrmIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcClient jdbcClient;

    private UUID testCompanyId;
    private Long testTariffId;
    private Long testProductId;

    @BeforeEach
    void setUp() {
        // Clean test data
        jdbcClient.sql("DELETE FROM customer_product_prices").update();
        jdbcClient.sql("DELETE FROM customers").update();
        jdbcClient.sql("DELETE FROM products").update();
        jdbcClient.sql("DELETE FROM taxes").update();
        jdbcClient.sql("DELETE FROM tariffs").update();
        jdbcClient.sql("DELETE FROM companies").update();

        // Create test company
        testCompanyId = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO companies (id, name, tax_id, is_active, created_at, updated_at, version)
                VALUES (?, 'Test Company', 'B11111111', TRUE, NOW(), NOW(), 1)
                """)
                .param(testCompanyId)
                .update();

        // Create test tariff
        testTariffId = jdbcClient.sql("""
                INSERT INTO tariffs (company_id, name, priority, price_type, is_default, active, version)
                VALUES (?, 'VIP Tariff', 10, 'RETAIL', FALSE, TRUE, 1) RETURNING id
                """)
                .param(testCompanyId)
                .query(Long.class)
                .single();

        // Create test tax
        Long taxId = jdbcClient.sql("""
                INSERT INTO taxes (company_id, name, rate, surcharge, active, created_at)
                VALUES (?, 'IVA 21%', 21.0, 0.0, TRUE, NOW()) RETURNING id
                """)
                .param(testCompanyId)
                .query(Long.class)
                .single();

        // Create test product
        UUID productUuid = UUID.randomUUID();
        testProductId = jdbcClient.sql("""
                INSERT INTO products (uuid, company_id, reference, name, short_name, tax_id,
                    type, is_weighted, is_inventoriable, average_cost, last_purchase_cost,
                    active, created_at, updated_at, version)
                VALUES (?, ?, 'PROD001', 'Test Product', 'TestProd', ?, 'PRODUCT', FALSE, TRUE,
                    0, 0, TRUE, NOW(), NOW(), 1) RETURNING id
                """)
                .param(productUuid)
                .param(testCompanyId)
                .param(taxId)
                .query(Long.class)
                .single();
    }

    @Test
    void shouldCreateCustomer() {
        // Given
        Map<String, Object> createRequest = Map.of(
                "companyId", testCompanyId.toString(),
                "legalName", "Test Customer SL",
                "taxId", "B98765432",
                "email", "customer@test.com",
                "phone", "+34912345678",
                "address", "Test Street 123",
                "zipCode", "28001",
                "city", "Madrid");

        // When
        @SuppressWarnings("rawtypes")
        ResponseEntity<ApiResponse<Map>> response = restTemplate.exchange(
                "/api/v1/customers",
                HttpMethod.POST,
                new HttpEntity<>(createRequest),
                new ParameterizedTypeReference<ApiResponse<Map>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        Map<String, Object> body = response.getBody().getData();
        assertThat(body).isNotNull();
        assertThat(body.get("legalName")).isEqualTo("Test Customer SL");
        assertThat(body.get("taxId")).isEqualTo("B98765432");
        assertThat(body.get("email")).isEqualTo("customer@test.com");

        // Verify persistence
        Integer count = jdbcClient.sql("SELECT COUNT(*) FROM customers WHERE tax_id = ?")
                .param("B98765432")
                .query(Integer.class)
                .single();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldSearchCustomers() {
        // Given
        createTestCustomer("Customer A", "B11111111");
        createTestCustomer("Customer B", "B22222222");
        createTestCustomer("Different Co", "B33333333");

        // When - search by name
        @SuppressWarnings("rawtypes")
        ResponseEntity<ApiResponse<List<Map>>> response = restTemplate.exchange(
                "/api/v1/customers?companyId=" + testCompanyId + "&search=Customer",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<List<Map>>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        List<Map> customers = response.getBody().getData();
        assertThat(customers).hasSize(2);
    }

    @Test
    void shouldListAllCustomers() {
        // Given
        createTestCustomer("Customer 1", "B11111111");
        createTestCustomer("Customer 2", "B22222222");

        // When
        @SuppressWarnings("rawtypes")
        ResponseEntity<ApiResponse<List<Map>>> response = restTemplate.exchange(
                "/api/v1/customers?companyId=" + testCompanyId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<List<Map>>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        List<Map> customers = response.getBody().getData();
        assertThat(customers).hasSize(2);
    }

    @Test
    void shouldUpdateCustomer() {
        // Given
        UUID customerId = createTestCustomer("Original Name", "B11111111");

        Map<String, Object> updateRequest = Map.of(
                "email", "updated@test.com",
                "phone", "+34999888777",
                "address", "New Address 456",
                "zipCode", "28002",
                "city", "Barcelona");

        // When
        @SuppressWarnings("rawtypes")
        ResponseEntity<ApiResponse<Map>> response = restTemplate.exchange(
                "/api/v1/customers/" + customerId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                new ParameterizedTypeReference<ApiResponse<Map>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        Map<String, Object> body = response.getBody().getData();
        assertThat(body).isNotNull();
        assertThat(body.get("email")).isEqualTo("updated@test.com");

        // Verify persistence
        String email = jdbcClient.sql("SELECT email FROM customers WHERE uuid = ?")
                .param(customerId)
                .query(String.class)
                .single();
        assertThat(email).isEqualTo("updated@test.com");
    }

    @Test
    void shouldUpdateCommercialTerms() {
        // Given
        UUID customerId = createTestCustomer("Test Customer", "B11111111");

        Map<String, Object> updateRequest = Map.of(
                "tariffId", testTariffId,
                "allowCredit", true,
                "creditLimitCents", 500000L);

        // When
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/customers/" + customerId + "/commercial-terms",
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                Void.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify persistence
        Long tariffId = jdbcClient.sql("""
                SELECT tariff_id FROM customers WHERE uuid = ?
                """)
                .param(customerId)
                .query(Long.class)
                .single();

        Boolean allowCredit = jdbcClient.sql("""
                SELECT allow_credit FROM customers WHERE uuid = ?
                """)
                .param(customerId)
                .query(Boolean.class)
                .single();

        Long creditLimit = jdbcClient.sql("""
                SELECT credit_limit FROM customers WHERE uuid = ?
                """)
                .param(customerId)
                .query(Long.class)
                .single();

        assertThat(tariffId).isEqualTo(testTariffId);
        assertThat(allowCredit).isTrue();
        assertThat(creditLimit).isEqualTo(500000L);
    }

    @Test
    void shouldUpdateSpecialPrices() {
        // Given
        UUID customerId = createTestCustomer("VIP Customer", "B11111111");

        Map<String, Object> updateRequest = Map.of(
                "prices", List.of(
                        Map.of("productId", testProductId, "priceCents", 8500L)));

        // When
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/customers/" + customerId + "/special-prices",
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                Void.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify persistence
        Long customPrice = jdbcClient.sql("""
                SELECT custom_price FROM customer_product_prices
                WHERE customer_id = (SELECT id FROM customers WHERE uuid = ?)
                AND product_id = ?
                """)
                .param(customerId)
                .param(testProductId)
                .query(Long.class)
                .single();

        assertThat(customPrice).isEqualTo(8500L);
    }

    @Test
    void shouldGetSpecialPrices() {
        // Given
        UUID customerId = createTestCustomer("Test Customer", "B11111111");
        Long customerIdLong = jdbcClient.sql("SELECT id FROM customers WHERE uuid = ?")
                .param(customerId)
                .query(Long.class)
                .single();

        jdbcClient.sql("""
                INSERT INTO customer_product_prices (customer_id, product_id, custom_price, valid_from, created_at)
                VALUES (?, ?, 9000, NOW(), NOW())
                """)
                .param(customerIdLong)
                .param(testProductId)
                .update();

        // When
        @SuppressWarnings("rawtypes")
        ResponseEntity<ApiResponse<List<Map>>> response = restTemplate.exchange(
                "/api/v1/customers/" + customerId + "/special-prices",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<List<Map>>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        List<Map> body = response.getBody().getData();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).get("priceCents")).isEqualTo(9000);
    }

    @Test
    void shouldEnforceTenantIsolation() {
        // Given - Create customer in another company
        UUID anotherCompanyId = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO companies (id, name, tax_id, is_active, created_at, updated_at, version)
                VALUES (?, 'Another Company', 'B99999999', TRUE, NOW(), NOW(), 1)
                """)
                .param(anotherCompanyId)
                .update();

        // Create customer for the other company
        UUID otherCustomerUuid = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO customers (uuid, company_id, tax_id, legal_name, commercial_name,
                    country, allow_credit, credit_limit, surcharge_apply, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'ES', FALSE, 0, FALSE, TRUE, NOW(), NOW())
                """)
                .param(otherCustomerUuid)
                .param(anotherCompanyId)
                .param("B88888888")
                .param("Other Company Customer")
                .param("Other Company Customer")
                .update();

        // When - List customers for test company only
        ResponseEntity<ApiResponse<List<Map>>> response = restTemplate.exchange(
                "/api/v1/customers?companyId=" + testCompanyId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<List<Map>>>() {
                });

        // Then - Should only see customers from test company
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        List<Map> customers = response.getBody().getData();
        assertThat(customers).isEmpty();
    }

    // ==================== HELPER METHODS ====================

    private UUID createTestCustomer(String legalName, String taxId) {
        UUID uuid = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO customers (uuid, company_id, tax_id, legal_name, commercial_name,
                    country, allow_credit, credit_limit, surcharge_apply, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'ES', FALSE, 0, FALSE, TRUE, NOW(), NOW())
                """)
                .param(uuid)
                .param(testCompanyId)
                .param(taxId)
                .param(legalName)
                .param(legalName)
                .update();
        return uuid;
    }
}
