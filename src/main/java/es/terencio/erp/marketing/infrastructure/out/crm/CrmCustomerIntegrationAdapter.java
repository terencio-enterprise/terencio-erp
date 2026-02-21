package es.terencio.erp.marketing.infrastructure.out.crm;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import es.terencio.erp.marketing.application.dto.customer.MarketingCustomer;
import es.terencio.erp.marketing.application.port.out.CustomerIntegrationPort;
import es.terencio.erp.marketing.domain.model.MarketingStatus;

@Repository
public class CrmCustomerIntegrationAdapter implements CustomerIntegrationPort {

    private final JdbcTemplate jdbcTemplate;

    public CrmCustomerIntegrationAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<MarketingCustomer> findByToken(String token) {
        String sql = """
            SELECT id,
                   email,
                   COALESCE(commercial_name, legal_name, 'Customer') AS name,
                   marketing_status,
                   unsubscribe_token,
                   active
            FROM customers
            WHERE unsubscribe_token = ?
              AND deleted_at IS NULL
            """;

        List<MarketingCustomer> results = jdbcTemplate.query(sql, this::mapRow, token);
        return results.stream().findFirst();
    }

    @Override
    public void updateMarketingStatus(String token, MarketingStatus status, Instant snoozeUntil) {
        String sql = """
            UPDATE customers
            SET marketing_status = ?,
                marketing_snooze_until = ?,
                updated_at = NOW()
            WHERE unsubscribe_token = ?
              AND deleted_at IS NULL
            """;

        jdbcTemplate.update(sql, status.name(), snoozeUntil, token);
    }

    private MarketingCustomer mapRow(ResultSet rs, int rowNum) throws SQLException {
        String statusStr = rs.getString("marketing_status");
        MarketingStatus status =
            statusStr != null ? MarketingStatus.valueOf(statusStr) : MarketingStatus.UNSUBSCRIBED;

        boolean active = rs.getBoolean("active");
        boolean canReceive = active && status == MarketingStatus.SUBSCRIBED;

        return new MarketingCustomer(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("name"),
            canReceive,
            status,
            rs.getString("unsubscribe_token")
        );
    }
}