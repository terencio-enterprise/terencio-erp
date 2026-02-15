package es.terencio.erp.stores.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.terencio.erp.stores.application.dto.StoreSettingsDto;
import es.terencio.erp.stores.application.port.out.StoreSettingsPort;

@Repository
public class JdbcStoreSettingsPersistenceAdapter implements StoreSettingsPort {

    private final JdbcClient jdbcClient;

    public JdbcStoreSettingsPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<StoreSettingsDto> findByStoreId(UUID storeId) {
        return jdbcClient.sql("""
                SELECT store_id, allow_negative_stock, default_tariff_id,
                       print_ticket_automatically, require_customer_for_large_amount
                FROM store_settings
                WHERE store_id = :storeId
                """)
                .param("storeId", storeId)
                .query((rs, rowNum) -> new StoreSettingsDto(
                        (UUID) rs.getObject("store_id"),
                        rs.getBoolean("allow_negative_stock"),
                        rs.getObject("default_tariff_id", Long.class),
                        rs.getBoolean("print_ticket_automatically"),
                        rs.getObject("require_customer_for_large_amount", Long.class)))
                .optional();
    }

    @Override
    public void save(StoreSettingsDto settings) {
        jdbcClient.sql("""
                INSERT INTO store_settings (store_id, allow_negative_stock, default_tariff_id,
                    print_ticket_automatically, require_customer_for_large_amount, updated_at)
                VALUES (:storeId, :allowNegativeStock, :defaultTariffId,
                    :printTicketAutomatically, :requireCustomerForLargeAmount, NOW())
                """)
                .param("storeId", settings.storeId())
                .param("allowNegativeStock", settings.allowNegativeStock())
                .param("defaultTariffId", settings.defaultTariffId())
                .param("printTicketAutomatically", settings.printTicketAutomatically())
                .param("requireCustomerForLargeAmount", settings.requireCustomerForLargeAmount())
                .update();
    }

    @Override
    public void update(StoreSettingsDto settings) {
        jdbcClient.sql("""
                UPDATE store_settings
                SET allow_negative_stock = :allowNegativeStock,
                    default_tariff_id = :defaultTariffId,
                    print_ticket_automatically = :printTicketAutomatically,
                    require_customer_for_large_amount = :requireCustomerForLargeAmount,
                    updated_at = NOW()
                WHERE store_id = :storeId
                """)
                .param("storeId", settings.storeId())
                .param("allowNegativeStock", settings.allowNegativeStock())
                .param("defaultTariffId", settings.defaultTariffId())
                .param("printTicketAutomatically", settings.printTicketAutomatically())
                .param("requireCustomerForLargeAmount", settings.requireCustomerForLargeAmount())
                .update();
    }
}
