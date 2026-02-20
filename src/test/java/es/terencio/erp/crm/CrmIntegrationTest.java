package es.terencio.erp.crm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.terencio.erp.AbstractIntegrationTest;
import es.terencio.erp.shared.presentation.ApiResponse;

class CrmIntegrationTest extends AbstractIntegrationTest {

        private Long testTariffId;
        private Long testProductId;

        @BeforeEach
        void setUp() {
                testTariffId = jdbcClient.sql(
                                "INSERT INTO tariffs (company_id, name, priority, price_type, is_default, active, version) VALUES (?, 'VIP Tariff', 10, 'RETAIL', FALSE, TRUE, 1) RETURNING id")
                                .param(globalCompanyId).query(Long.class).single();

                Long taxId = jdbcClient.sql(
                                "INSERT INTO taxes (company_id, name, rate, surcharge, active, created_at) VALUES (?, 'IVA 21%', 21.0, 0.0, TRUE, NOW()) RETURNING id")
                                .param(globalCompanyId).query(Long.class).single();

                UUID productUuid = UUID.randomUUID();
                testProductId = jdbcClient.sql(
                                "INSERT INTO products (uuid, company_id, reference, name, short_name, tax_id, type, is_weighted, is_inventoriable, average_cost, last_purchase_cost, active, created_at, updated_at, version) VALUES (?, ?, 'PROD001', 'Test Product', 'TestProd', ?, 'PRODUCT', FALSE, TRUE, 0, 0, TRUE, NOW(), NOW(), 1) RETURNING id")
                                .param(productUuid).param(globalCompanyId).param(taxId).query(Long.class).single();
        }

        @Test
        void shouldCreateCustomer() {
                Map<String, Object> createRequest = Map.of(
                                "companyId", globalCompanyId.toString(),
                                "legalName", "Test Customer SL",
                                "taxId", "B98765432",
                                "email", "customer@test.com",
                                "phone", "+34912345678",
                                "address", "Test Street 123",
                                "zipCode", "28001",
                                "city", "Madrid");

                @SuppressWarnings("rawtypes")
                ResponseEntity<ApiResponse<Map>> response = restTemplate.exchange(
                                "/api/v1/customers",
                                HttpMethod.POST,
                                new HttpEntity<>(createRequest, globalAdminHeaders),
                                new ParameterizedTypeReference<ApiResponse<Map>>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().isSuccess()).isTrue();

                Integer count = jdbcClient.sql("SELECT COUNT(*) FROM customers WHERE tax_id = ?")
                                .param("B98765432").query(Integer.class).single();
                assertThat(count).isEqualTo(1);
        }

        @Test
        void shouldSearchCustomers() {
                createTestCustomer("Customer A", "B11111111");
                createTestCustomer("Customer B", "B22222222");

                @SuppressWarnings("rawtypes")
                ResponseEntity<ApiResponse<List<Map>>> response = restTemplate.exchange(
                                "/api/v1/customers?companyId=" + globalCompanyId + "&search=Customer",
                                HttpMethod.GET,
                                new HttpEntity<>(globalAdminHeaders),
                                new ParameterizedTypeReference<ApiResponse<List<Map>>>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().getData()).hasSize(2);
        }

        @Test
        void shouldListAllCustomers() {
                createTestCustomer("Customer 1", "B11111111");
                createTestCustomer("Customer 2", "B22222222");

                @SuppressWarnings("rawtypes")
                ResponseEntity<ApiResponse<List<Map>>> response = restTemplate.exchange(
                                "/api/v1/customers?companyId=" + globalCompanyId,
                                HttpMethod.GET,
                                new HttpEntity<>(globalAdminHeaders),
                                new ParameterizedTypeReference<ApiResponse<List<Map>>>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().getData()).hasSize(2);
        }

        @Test
        void shouldUpdateCustomer() {
                UUID customerId = createTestCustomer("Original Name", "B11111111");

                Map<String, Object> updateRequest = Map.of(
                                "email", "updated@test.com",
                                "phone", "+34999888777",
                                "address", "New Address 456",
                                "zipCode", "28002",
                                "city", "Barcelona");

                @SuppressWarnings("rawtypes")
                ResponseEntity<ApiResponse<Map>> response = restTemplate.exchange(
                                "/api/v1/customers/" + customerId + "?companyId=" + globalCompanyId,
                                HttpMethod.PUT,
                                new HttpEntity<>(updateRequest, globalAdminHeaders),
                                new ParameterizedTypeReference<ApiResponse<Map>>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

                String email = jdbcClient.sql("SELECT email FROM customers WHERE uuid = ?")
                                .param(customerId).query(String.class).single();
                assertThat(email).isEqualTo("updated@test.com");
        }

        @Test
        void shouldUpdateCommercialTerms() {
                UUID customerId = createTestCustomer("Test Customer", "B11111111");

                Map<String, Object> updateRequest = Map.of(
                                "tariffId", testTariffId,
                                "allowCredit", true,
                                "creditLimitCents", 500000L);

                ResponseEntity<Void> response = restTemplate.exchange(
                                "/api/v1/customers/" + customerId + "/commercial-terms?companyId=" + globalCompanyId,
                                HttpMethod.PUT,
                                new HttpEntity<>(updateRequest, globalAdminHeaders),
                                Void.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

                Long tariffId = jdbcClient.sql("SELECT tariff_id FROM customers WHERE uuid = ?").param(customerId)
                                .query(Long.class).single();
                Boolean allowCredit = jdbcClient.sql("SELECT allow_credit FROM customers WHERE uuid = ?")
                                .param(customerId).query(Boolean.class).single();

                assertThat(tariffId).isEqualTo(testTariffId);
                assertThat(allowCredit).isTrue();
        }

        @Test
        void shouldUpdateSpecialPrices() {
                UUID customerId = createTestCustomer("VIP Customer", "B11111111");

                Map<String, Object> updateRequest = Map.of(
                                "prices", List.of(Map.of("productId", testProductId, "priceCents", 8500L)));

                ResponseEntity<Void> response = restTemplate.exchange(
                                "/api/v1/customers/" + customerId + "/special-prices?companyId=" + globalCompanyId,
                                HttpMethod.PUT,
                                new HttpEntity<>(updateRequest, globalAdminHeaders),
                                Void.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

                Long customPrice = jdbcClient.sql(
                                "SELECT custom_price FROM customer_product_prices WHERE customer_id = (SELECT id FROM customers WHERE uuid = ?) AND product_id = ?")
                                .param(customerId).param(testProductId).query(Long.class).single();

                assertThat(customPrice).isEqualTo(8500L);
        }

        @Test
        void shouldGetSpecialPrices() {
                UUID customerId = createTestCustomer("Test Customer", "B11111111");
                Long customerIdLong = jdbcClient.sql("SELECT id FROM customers WHERE uuid = ?").param(customerId)
                                .query(Long.class).single();

                jdbcClient.sql("INSERT INTO customer_product_prices (customer_id, product_id, custom_price, valid_from, created_at) VALUES (?, ?, 9000, NOW(), NOW())")
                                .param(customerIdLong).param(testProductId).update();

                @SuppressWarnings("rawtypes")
                ResponseEntity<ApiResponse<List<Map>>> response = restTemplate.exchange(
                                "/api/v1/customers/" + customerId + "/special-prices?companyId=" + globalCompanyId,
                                HttpMethod.GET,
                                new HttpEntity<>(globalAdminHeaders),
                                new ParameterizedTypeReference<ApiResponse<List<Map>>>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().getData()).hasSize(1);
                assertThat(response.getBody().getData().get(0).get("priceCents")).isEqualTo(9000);
        }
}