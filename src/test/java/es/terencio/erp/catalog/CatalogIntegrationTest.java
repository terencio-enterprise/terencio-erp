package es.terencio.erp.catalog;

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
 * Integration tests for Catalog module (Products, Categories, Taxes, Tariffs,
 * Prices).
 */
class CatalogIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcClient jdbcClient;

    private UUID testCompanyId;
    private Long testTaxId;

    @BeforeEach
    void setUp() {
        // Clean test data
        jdbcClient.sql("DELETE FROM product_prices").update();
        jdbcClient.sql("DELETE FROM product_barcodes").update();
        jdbcClient.sql("DELETE FROM products").update();
        jdbcClient.sql("DELETE FROM categories").update();
        jdbcClient.sql("DELETE FROM taxes").update();
        jdbcClient.sql("DELETE FROM tariffs").update();
        jdbcClient.sql("DELETE FROM companies").update();

        // Create test company
        testCompanyId = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO companies (id, name, tax_id, currency_code, is_active, created_at, updated_at, version)
                VALUES (?, 'Test Company', 'B11111111', 'EUR', TRUE, NOW(), NOW(), 1)
                """)
                .param(testCompanyId)
                .update();

        // Create test tax
        testTaxId = jdbcClient.sql("""
                INSERT INTO taxes (company_id, name, rate, surcharge, code_aeat, active, created_at)
                VALUES (?, 'IVA 21%', 21.0, 0.0, '21', TRUE, NOW())
                RETURNING id
                """)
                .param(testCompanyId)
                .query(Long.class)
                .single();
    }

    @Test
    void shouldListTaxes() {
        // When
        ResponseEntity<ApiResponse<List<Map>>> response = restTemplate.exchange(
                "/api/v1/catalog/taxes?companyId=" + testCompanyId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<List<Map>>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        List<Map> taxes = response.getBody().getData();
        assertThat(taxes).hasSize(1);
        assertThat(taxes.get(0).get("name")).isEqualTo("IVA 21%");
        assertThat(taxes.get(0).get("rate")).isEqualTo(21.0);
    }

    @Test
    void shouldCreateCategory() {
        // Given
        Map<String, Object> createRequest = Map.of(
                "companyId", testCompanyId.toString(),
                "name", "Electronics");

        // When
        ResponseEntity<ApiResponse<Map>> response = restTemplate.exchange(
                "/api/v1/catalog/categories",
                HttpMethod.POST,
                new HttpEntity<>(createRequest),
                new ParameterizedTypeReference<ApiResponse<Map>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        Map category = response.getBody().getData();
        assertThat(category.get("name")).isEqualTo("Electronics");

        // Verify persistence
        Integer count = jdbcClient.sql("SELECT COUNT(*) FROM categories WHERE company_id = ?")
                .param(testCompanyId)
                .query(Integer.class)
                .single();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldListCategories() {
        // Given
        createTestCategory("Category A");
        createTestCategory("Category B");

        // When
        ResponseEntity<ApiResponse<List<Map>>> response = restTemplate.exchange(
                "/api/v1/catalog/categories?companyId=" + testCompanyId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<List<Map>>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        List<Map> categories = response.getBody().getData();
        assertThat(categories).hasSize(2);
    }

    @Test
    void shouldCreateProduct() {
        // Given
        Long categoryId = createTestCategory("Test Category");

        Map<String, Object> createRequest = Map.of(
                "companyId", testCompanyId.toString(),
                "reference", "PROD001",
                "name", "Test Product",
                "shortName", "TestProd",
                "taxId", testTaxId,
                "type", "PRODUCT");

        // When
        ResponseEntity<ApiResponse<Map>> response = restTemplate.exchange(
                "/api/v1/catalog/products",
                HttpMethod.POST,
                new HttpEntity<>(createRequest),
                new ParameterizedTypeReference<ApiResponse<Map>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        Map product = response.getBody().getData();
        assertThat(product.get("reference")).isEqualTo("PROD001");
        assertThat(product.get("name")).isEqualTo("Test Product");

        // Verify persistence
        Integer count = jdbcClient.sql("SELECT COUNT(*) FROM products WHERE reference = ?")
                .param("PROD001")
                .query(Integer.class)
                .single();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldSearchProducts() {
        // Given
        createTestProduct("PROD001", "Laptop Dell");
        createTestProduct("PROD002", "Laptop HP");
        createTestProduct("PROD003", "Mouse Logitech");

        // When - search by name
        ResponseEntity<ApiResponse<List<Map>>> response = restTemplate.exchange(
                "/api/v1/catalog/products?companyId=" + testCompanyId + "&name=Laptop",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<List<Map>>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        List<Map> products = response.getBody().getData();
        assertThat(products).hasSize(2);
    }

    @Test
    void shouldUpdateProduct() {
        // Given
        Long productId = createTestProduct("PROD001", "Original Name");

        Map<String, Object> updateRequest = Map.of(
                "name", "Updated Product Name",
                "shortName", "UpdatedProd",
                "description", "Updated description");

        // When
        ResponseEntity<ApiResponse<Map>> response = restTemplate.exchange(
                "/api/v1/catalog/products/" + productId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                new ParameterizedTypeReference<ApiResponse<Map>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        Map product = response.getBody().getData();
        assertThat(product.get("name")).isEqualTo("Updated Product Name");

        // Verify persistence
        String name = jdbcClient.sql("SELECT name FROM products WHERE id = ?")
                .param(productId)
                .query(String.class)
                .single();
        assertThat(name).isEqualTo("Updated Product Name");
    }

    @Test
    void shouldListTariffs() {
        // Given
        createTestTariff("PVP General", true);
        createTestTariff("Wholesale", false);

        // When
        ResponseEntity<ApiResponse<List<Map>>> response = restTemplate.exchange(
                "/api/v1/catalog/tariffs?companyId=" + testCompanyId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<List<Map>>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        List<Map> tariffs = response.getBody().getData();
        assertThat(tariffs).hasSize(2);
    }

    @Test
    void shouldUpdateProductPrices() {
        // Given
        Long productId = createTestProduct("PROD001", "Test Product");
        Long tariff1 = createTestTariff("Tariff 1", true);
        Long tariff2 = createTestTariff("Tariff 2", false);

        Map<String, Object> updateRequest = Map.of(
                "prices", List.of(
                        Map.of("tariffId", tariff1, "priceCents", 10000L),
                        Map.of("tariffId", tariff2, "priceCents", 8000L)));

        // When
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/catalog/products/" + productId + "/prices",
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                Void.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify persistence
        Integer count = jdbcClient.sql("SELECT COUNT(*) FROM product_prices WHERE product_id = ?")
                .param(productId)
                .query(Integer.class)
                .single();
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldGetProductPrices() {
        // Given
        Long productId = createTestProduct("PROD001", "Test Product");
        Long tariffId = createTestTariff("Test Tariff", true);
        createTestProductPrice(productId, tariffId, 15000L);

        // When
        ResponseEntity<ApiResponse<List<Map>>> response = restTemplate.exchange(
                "/api/v1/catalog/products/" + productId + "/prices?companyId=" + testCompanyId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<List<Map>>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        List<Map> prices = response.getBody().getData();
        assertThat(prices).hasSize(1);
        assertThat(prices.get(0).get("priceCents")).isEqualTo(15000);
    }

    // ==================== HELPER METHODS ====================

    private Long createTestCategory(String name) {
        return jdbcClient.sql("""
                INSERT INTO categories (company_id, name, active) VALUES (?, ?, TRUE) RETURNING id
                """)
                .param(testCompanyId)
                .param(name)
                .query(Long.class)
                .single();
    }

    private Long createTestProduct(String reference, String name) {
        UUID productUuid = UUID.randomUUID();
        return jdbcClient.sql("""
                INSERT INTO products (uuid, company_id, reference, name, short_name,
                    tax_id, type, is_weighted, is_inventoriable, min_stock_alert,
                    average_cost, last_purchase_cost, active, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, ?, 'PRODUCT', FALSE, TRUE, 0, 0, 0, TRUE, NOW(), NOW(), 1)
                RETURNING id
                """)
                .param(productUuid)
                .param(testCompanyId)
                .param(reference)
                .param(name)
                .param(name)
                .param(testTaxId)
                .query(Long.class)
                .single();
    }

    private Long createTestTariff(String name, boolean isDefault) {
        return jdbcClient.sql("""
                INSERT INTO tariffs (company_id, name, priority, price_type, is_default, active, version)
                VALUES (?, ?, 0, 'RETAIL', ?, TRUE, 1) RETURNING id
                """)
                .param(testCompanyId)
                .param(name)
                .param(isDefault)
                .query(Long.class)
                .single();
    }

    private void createTestProductPrice(Long productId, Long tariffId, long priceCents) {
        jdbcClient.sql("""
                INSERT INTO product_prices (product_id, tariff_id, price, updated_at)
                VALUES (?, ?, ?, NOW())
                """)
                .param(productId)
                .param(tariffId)
                .param(priceCents)
                .update();
    }
}
