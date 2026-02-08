package es.terencio.erp.sync.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.terencio.erp.sync.application.dto.SyncBarcodeDto;
import es.terencio.erp.sync.application.dto.SyncCustomerDto;
import es.terencio.erp.sync.application.dto.SyncPriceDto;
import es.terencio.erp.sync.application.dto.SyncProductDto;
import es.terencio.erp.sync.application.dto.SyncPromotionDto;
import es.terencio.erp.sync.application.dto.SyncResponseDto;
import es.terencio.erp.sync.application.dto.SyncTariffDto;
import es.terencio.erp.sync.application.dto.SyncTaxDto;
import es.terencio.erp.sync.application.dto.SyncUserDto;
import es.terencio.erp.sync.application.port.out.LoadSyncDataPort;

@Repository
public class JdbcSyncAdapter implements LoadSyncDataPort {

    private final JdbcClient jdbcClient;

    public JdbcSyncAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public SyncResponseDto fetchChanges(UUID storeId, Instant fromTime) {
        Timestamp ts = Timestamp.from(fromTime);
        Instant now = Instant.now();

        // 1. TAXES (Usually global, but checking validity)
        // Note: Taxes don't always have updated_at in all schemas, assuming active check or simple fetch
        var taxes = jdbcClient.sql("""
                SELECT id, name, rate, surcharge, code_aeat, active
                FROM taxes
                WHERE (store_id IS NULL OR store_id = :storeId)
                """)
                .param("storeId", storeId)
                .query(SyncTaxDto.class)
                .list();

        // 2. TARIFFS
        var tariffs = jdbcClient.sql("""
                SELECT id, name, priority, active
                FROM tariffs
                WHERE (store_id IS NULL OR store_id = :storeId)
                """)
                .param("storeId", storeId)
                .query(SyncTariffDto.class)
                .list();

        // 3. PRODUCTS (Global OR Store Specific)
        // We fetch products modified after the timestamp
        var products = jdbcClient.sql("""
                SELECT id, uuid, reference, name, short_name, description, family_code,
                       tax_id, is_weighted, is_service, is_age_restricted, requires_manager,
                       stock_tracking, min_stock_alert, image_url, active, updated_at
                FROM products
                WHERE (store_id IS NULL OR store_id = :storeId)
                  AND updated_at > :ts
                """)
                .param("storeId", storeId)
                .param("ts", ts)
                .query(SyncProductDto.class)
                .list();

        // 4. BARCODES
        // We fetch barcodes if the barcode itself was created recently OR if the parent product changed
        var barcodes = jdbcClient.sql("""
                SELECT b.barcode, b.product_id, b.type, b.is_primary, b.quantity_factor
                FROM product_barcodes b
                JOIN products p ON b.product_id = p.id
                WHERE (p.store_id IS NULL OR p.store_id = :storeId)
                  AND (b.created_at > :ts OR p.updated_at > :ts)
                """)
                .param("storeId", storeId)
                .param("ts", ts)
                .query(SyncBarcodeDto.class)
                .list();

        // 5. PRICES
        // Prices change if the price table updates OR if the product updates
        var prices = jdbcClient.sql("""
                SELECT pp.product_id, pp.tariff_id, pp.price
                FROM product_prices pp
                JOIN products p ON pp.product_id = p.id
                WHERE (p.store_id IS NULL OR p.store_id = :storeId)
                  AND (pp.updated_at > :ts OR p.updated_at > :ts)
                """)
                .param("storeId", storeId)
                .param("ts", ts)
                .query(SyncPriceDto.class)
                .list();

        // 6. PROMOTIONS
        var promotions = jdbcClient.sql("""
                SELECT id, name, type, start_date, end_date, priority, rules_json, active
                FROM promotions
                WHERE (store_id IS NULL OR store_id = :storeId)
                  AND created_at > :ts
                """)
                .param("storeId", storeId)
                .param("ts", ts)
                .query(SyncPromotionDto.class)
                .list();

        // 7. CUSTOMERS
        var customers = jdbcClient.sql("""
                SELECT id, uuid, tax_id, legal_name, commercial_name, address, zip_code,
                       email, phone, tariff_id, allow_credit, credit_limit, surcharge_apply,
                       verifactu_ref, active, updated_at
                FROM customers
                WHERE (store_id IS NULL OR store_id = :storeId)
                  AND updated_at > :ts
                """)
                .param("storeId", storeId)
                .param("ts", ts)
                .query(SyncCustomerDto.class)
                .list();

        // 8. USERS (Staff for this store)
        var users = jdbcClient.sql("""
                SELECT id, username, pin_hash, full_name, role, permissions_json, is_active, updated_at
                FROM users
                WHERE store_id = :storeId
                  AND updated_at > :ts
                """)
                .param("storeId", storeId)
                .param("ts", ts)
                .query(SyncUserDto.class)
                .list();

        return new SyncResponseDto(
                now,
                taxes,
                tariffs,
                products,
                barcodes,
                prices,
                promotions,
                customers,
                users
        );
    }
}
