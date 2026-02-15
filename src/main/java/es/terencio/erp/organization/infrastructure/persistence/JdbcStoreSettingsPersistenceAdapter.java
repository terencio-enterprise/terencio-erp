package es.terencio.erp.organization.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.terencio.erp.organization.application.port.out.StoreSettingsRepository;
import es.terencio.erp.organization.domain.model.StoreSettings;
import es.terencio.erp.shared.domain.identifier.StoreId;
import es.terencio.erp.shared.domain.valueobject.Money;

/**
 * JDBC adapter for StoreSettings persistence.
 */
@Repository("organizationStoreSettingsAdapter")
public class JdbcStoreSettingsPersistenceAdapter implements StoreSettingsRepository {

    private final JdbcClient jdbcClient;

    public JdbcStoreSettingsPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public StoreSettings save(StoreSettings settings) {
        jdbcClient.sql("""
                INSERT INTO store_settings (store_id, allow_negative_stock, default_tariff_id,
                    print_ticket_automatically, require_customer_for_large_amount, updated_at)
                VALUES (:storeId, :allowNegativeStock, :defaultTariffId,
                    :printTicketAutomatically, :requireCustomerForLargeAmount, :updatedAt)
                ON CONFLICT (store_id) DO UPDATE SET
                    allow_negative_stock = EXCLUDED.allow_negative_stock,
                    default_tariff_id = EXCLUDED.default_tariff_id,
                    print_ticket_automatically = EXCLUDED.print_ticket_automatically,
                    require_customer_for_large_amount = EXCLUDED.require_customer_for_large_amount,
                    updated_at = EXCLUDED.updated_at
                """)
                .param("storeId", settings.storeId().value())
                .param("allowNegativeStock", settings.allowNegativeStock())
                .param("defaultTariffId", settings.defaultTariffId())
                .param("printTicketAutomatically", settings.printTicketAutomatically())
                .param("requireCustomerForLargeAmount", settings.requireCustomerForLargeAmount().cents())
                .param("updatedAt", Timestamp.from(settings.updatedAt()))
                .update();
        return settings;
    }

    @Override
    public Optional<StoreSettings> findByStoreId(StoreId storeId) {
        return jdbcClient.sql("""
                SELECT * FROM store_settings WHERE store_id = :storeId
                """)
                .param("storeId", storeId.value())
                .query(this::mapRow)
                .optional();
    }

    private StoreSettings mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new StoreSettings(
                new StoreId((UUID) rs.getObject("store_id")),
                rs.getBoolean("allow_negative_stock"),
                (Long) rs.getObject("default_tariff_id"),
                rs.getBoolean("print_ticket_automatically"),
                Money.ofEurosCents(rs.getLong("require_customer_for_large_amount")),
                rs.getTimestamp("updated_at").toInstant());
    }
}
