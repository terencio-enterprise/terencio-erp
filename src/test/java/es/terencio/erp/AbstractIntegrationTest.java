package es.terencio.erp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import es.terencio.erp.config.TestSecurityConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected JdbcClient jdbcClient;

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:17-alpine"));

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    /**
     * Deletes all data from all tables in the correct order to respect foreign key
     * constraints.
     * Call this method in @BeforeEach to ensure a clean state for each test.
     */
    protected void cleanDatabase() {
        // Level 1: Most dependent tables (leaf nodes)
        jdbcClient.sql("DELETE FROM audit_user_actions").update();
        jdbcClient.sql("DELETE FROM marketing_bounce_events").update();
        jdbcClient.sql("DELETE FROM marketing_logs").update();
        jdbcClient.sql("DELETE FROM marketing_attachments").update();
        jdbcClient.sql("DELETE FROM marketing_templates").update();
        jdbcClient.sql("DELETE FROM accounting_entry_lines").update();
        jdbcClient.sql("DELETE FROM fiscal_audit_log").update();
        jdbcClient.sql("DELETE FROM payments").update();
        jdbcClient.sql("DELETE FROM sale_taxes").update();
        jdbcClient.sql("DELETE FROM sale_lines").update();
        jdbcClient.sql("DELETE FROM cash_movements").update();
        jdbcClient.sql("DELETE FROM customer_account_movements").update();
        jdbcClient.sql("DELETE FROM stock_movements").update();
        jdbcClient.sql("DELETE FROM inventory_stock").update();
        jdbcClient.sql("DELETE FROM customer_product_prices").update();
        jdbcClient.sql("DELETE FROM product_prices").update();
        jdbcClient.sql("DELETE FROM product_barcodes").update();

        // Level 2: Middle layer tables
        jdbcClient.sql("DELETE FROM sales").update();
        jdbcClient.sql("DELETE FROM accounting_entries").update();
        jdbcClient.sql("DELETE FROM products").update();
        jdbcClient.sql("DELETE FROM customers").update();

        // Level 3: Infrastructure tables
        jdbcClient.sql("DELETE FROM shifts").update();
        jdbcClient.sql("DELETE FROM registration_codes").update();
        jdbcClient.sql("DELETE FROM devices").update();
        jdbcClient.sql("DELETE FROM users").update();
        jdbcClient.sql("DELETE FROM warehouses").update();
        jdbcClient.sql("DELETE FROM store_settings").update();
        jdbcClient.sql("DELETE FROM stores").update();

        // Level 4: Company-level configuration
        jdbcClient.sql("DELETE FROM pricing_rules").update();
        jdbcClient.sql("DELETE FROM categories").update();
        jdbcClient.sql("DELETE FROM taxes").update();
        jdbcClient.sql("DELETE FROM tariffs").update();
        jdbcClient.sql("DELETE FROM payment_methods").update();

        // Level 5: Root tables
        jdbcClient.sql("DELETE FROM companies").update();
    }
}