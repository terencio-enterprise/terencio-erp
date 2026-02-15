package es.terencio.erp.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
 * Integration tests for Inventory module (Stock movements, Stock projections).
 */
class InventoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcClient jdbcClient;

    private UUID testCompanyId;
    private UUID testWarehouseId;
    private Long testProductId;

    @BeforeEach
    void setUp() {
        // Clean test data
        jdbcClient.sql("DELETE FROM stock_movements").update();
        jdbcClient.sql("DELETE FROM inventory_stock").update();
        jdbcClient.sql("DELETE FROM products").update();
        jdbcClient.sql("DELETE FROM taxes").update();
        jdbcClient.sql("DELETE FROM warehouses").update();
        jdbcClient.sql("DELETE FROM stores").update();
        jdbcClient.sql("DELETE FROM companies").update();

        // Create test company
        testCompanyId = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO companies (id, name, tax_id, is_active, created_at, updated_at, version)
                VALUES (?, 'Test Company', 'B11111111', TRUE, NOW(), NOW(), 1)
                """)
                .param(testCompanyId)
                .update();

        // Create test store and warehouse
        UUID storeId = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO stores (id, company_id, code, name, is_active, timezone, created_at, updated_at, version)
                VALUES (?, ?, 'STORE001', 'Test Store', TRUE, 'Europe/Madrid', NOW(), NOW(), 1)
                """)
                .param(storeId)
                .param(testCompanyId)
                .update();

        testWarehouseId = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO warehouses (id, store_id, name, code, created_at)
                VALUES (?, ?, 'Main Warehouse', 'WH-001', NOW())
                """)
                .param(testWarehouseId)
                .param(storeId)
                .update();

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
    void shouldCreateInitialAdjustment() {
        // Given
        Map<String, Object> adjustmentRequest = Map.of(
                "companyId", testCompanyId.toString(),
                "productId", testProductId,
                "warehouseId", testWarehouseId.toString(),
                "adjustmentQuantity", new BigDecimal("100.00"),
                "reason", "Initial inventory");

        // When
        ResponseEntity<ApiResponse<Map>> response = restTemplate.exchange(
                "/api/v1/inventory/adjustments",
                HttpMethod.POST,
                new HttpEntity<>(adjustmentRequest),
                new ParameterizedTypeReference<ApiResponse<Map>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        Map result = response.getBody().getData();
        assertThat(result.get("previousBalance")).isEqualTo(0.0);
        assertThat(result.get("newBalance")).isEqualTo(100.0);

        // Verify stock movement created
        Integer movementCount = jdbcClient.sql("""
                SELECT COUNT(*) FROM stock_movements
                WHERE product_id = ? AND warehouse_id = ? AND type = 'ADJUSTMENT'
                """)
                .param(testProductId)
                .param(testWarehouseId)
                .query(Integer.class)
                .single();
        assertThat(movementCount).isEqualTo(1);

        // Verify stock projection updated
        BigDecimal quantityOnHand = jdbcClient.sql("""
                SELECT quantity_on_hand FROM inventory_stock
                WHERE product_id = ? AND warehouse_id = ?
                """)
                .param(testProductId)
                .param(testWarehouseId)
                .query(BigDecimal.class)
                .single();
        assertThat(quantityOnHand).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldGetStockByWarehouse() {
        // Given
        initializeStock(testProductId, testWarehouseId, new BigDecimal("50.00"));

        // When
        ResponseEntity<ApiResponse<List<Map>>> response = restTemplate.exchange(
                "/api/v1/inventory/stock?companyId=" + testCompanyId +
                        "&warehouseId=" + testWarehouseId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<List<Map>>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        List<Map> stock = response.getBody().getData();
        assertThat(stock).hasSize(1);
        assertThat(stock.get(0).get("quantityOnHand")).isEqualTo(50.0);
    }

    @Test
    void shouldGetProductMovements() {
        // Given
        createMovement(testProductId, new BigDecimal("100.00"), "ADJUSTMENT", "Initial");
        createMovement(testProductId, new BigDecimal("-10.00"), "SALE", "Sale");
        createMovement(testProductId, new BigDecimal("5.00"), "RETURN", "Return");

        // When
        ResponseEntity<ApiResponse<List<Map>>> response = restTemplate.exchange(
                "/api/v1/inventory/products/" + testProductId + "/movements",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<List<Map>>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        List<Map> movements = response.getBody().getData();
        assertThat(movements).hasSize(3);

        // Verify order (newest first)
        assertThat(movements.get(0).get("type")).isEqualTo("RETURN");
        assertThat(movements.get(1).get("type")).isEqualTo("SALE");
        assertThat(movements.get(2).get("type")).isEqualTo("ADJUSTMENT");
    }

    @Test
    void shouldAdjustStockWithNegativeQuantity() {
        // Given - Initialize with 100 units
        initializeStock(testProductId, testWarehouseId, new BigDecimal("100.00"));

        // When - Adjust by -30 (reduction)
        Map<String, Object> adjustmentRequest = Map.of(
                "companyId", testCompanyId.toString(),
                "productId", testProductId,
                "warehouseId", testWarehouseId.toString(),
                "adjustmentQuantity", new BigDecimal("-30.00"),
                "reason", "Breakage");

        ResponseEntity<ApiResponse<Map>> response = restTemplate.exchange(
                "/api/v1/inventory/adjustments",
                HttpMethod.POST,
                new HttpEntity<>(adjustmentRequest),
                new ParameterizedTypeReference<ApiResponse<Map>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        Map result = response.getBody().getData();
        assertThat(result.get("previousBalance")).isEqualTo(100.0);
        assertThat(result.get("newBalance")).isEqualTo(70.0);

        // Verify new balance in stock
        BigDecimal quantityOnHand = jdbcClient.sql("""
                SELECT quantity_on_hand FROM inventory_stock
                WHERE product_id = ? AND warehouse_id = ?
                """)
                .param(testProductId)
                .param(testWarehouseId)
                .query(BigDecimal.class)
                .single();
        assertThat(quantityOnHand).isEqualByComparingTo("70.00");
    }

    @Test
    void shouldTrackStockMovementKardex() {
        // Given - Sequence of movements
        initializeStock(testProductId, testWarehouseId, BigDecimal.ZERO);

        // Initial inventory
        createMovement(testProductId, new BigDecimal("100.00"), "ADJUSTMENT", "Initial stock");

        // Multiple sales
        createMovement(testProductId, new BigDecimal("-20.00"), "SALE", "Sale 1");
        createMovement(testProductId, new BigDecimal("-15.00"), "SALE", "Sale 2");

        // Return
        createMovement(testProductId, new BigDecimal("5.00"), "RETURN", "Customer return");

        // When
        ResponseEntity<ApiResponse<List<Map>>> response = restTemplate.exchange(
                "/api/v1/inventory/products/" + testProductId + "/movements",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<List<Map>>>() {
                });

        // Then - Verify all movements recorded
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        List<Map> movements = response.getBody().getData();
        assertThat(movements).hasSize(4);

        // Verify final stock balance
        BigDecimal finalStock = jdbcClient.sql("""
                SELECT quantity_on_hand FROM inventory_stock
                WHERE product_id = ? AND warehouse_id = ?
                """)
                .param(testProductId)
                .param(testWarehouseId)
                .query(BigDecimal.class)
                .single();
        assertThat(finalStock).isEqualByComparingTo("70.00"); // 100 - 20 - 15 + 5 = 70
    }

    // ==================== HELPER METHODS ====================

    private void initializeStock(Long productId, UUID warehouseId, BigDecimal quantity) {
        jdbcClient
                .sql("""
                        INSERT INTO inventory_stock (product_id, warehouse_id, company_id, quantity_on_hand, last_updated_at, version)
                        VALUES (?, ?, ?, ?, NOW(), 1)
                        ON CONFLICT (product_id, warehouse_id) DO UPDATE SET quantity_on_hand = EXCLUDED.quantity_on_hand
                        """)
                .param(productId)
                .param(warehouseId)
                .param(testCompanyId)
                .param(quantity)
                .update();
    }

    private void createMovement(Long productId, BigDecimal quantity, String type, String reason) {
        // Get current stock
        BigDecimal previousBalance = jdbcClient.sql("""
                SELECT COALESCE(quantity_on_hand, 0) FROM inventory_stock
                WHERE product_id = ? AND warehouse_id = ?
                """)
                .param(productId)
                .param(testWarehouseId)
                .query(BigDecimal.class)
                .optional()
                .orElse(BigDecimal.ZERO);

        BigDecimal newBalance = previousBalance.add(quantity);

        // Create movement
        UUID movementUuid = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO stock_movements (uuid, product_id, warehouse_id, type, quantity,
                    previous_balance, new_balance, reason, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """)
                .param(movementUuid)
                .param(productId)
                .param(testWarehouseId)
                .param(type)
                .param(quantity)
                .param(previousBalance)
                .param(newBalance)
                .param(reason)
                .update();

        // Update stock
        jdbcClient
                .sql("""
                        INSERT INTO inventory_stock (product_id, warehouse_id, company_id, quantity_on_hand, last_updated_at, version)
                        VALUES (?, ?, ?, ?, NOW(), 1)
                        ON CONFLICT (product_id, warehouse_id) DO UPDATE SET
                            quantity_on_hand = EXCLUDED.quantity_on_hand,
                            last_updated_at = EXCLUDED.last_updated_at
                        """)
                .param(productId)
                .param(testWarehouseId)
                .param(testCompanyId)
                .param(newBalance)
                .update();
    }
}
