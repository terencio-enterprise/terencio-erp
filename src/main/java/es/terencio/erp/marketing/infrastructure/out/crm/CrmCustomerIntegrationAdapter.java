package es.terencio.erp.marketing.infrastructure.out.crm;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import es.terencio.erp.marketing.application.dto.MarketingDtos.AudienceFilter;
import es.terencio.erp.marketing.application.port.out.CustomerIntegrationPort;

@Component
public class CrmCustomerIntegrationAdapter implements CustomerIntegrationPort {

    private final JdbcClient jdbcClient;

    public CrmCustomerIntegrationAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<MarketingCustomer> findAudience(Long campaignId, int limit, int page) {
        int offset = page * limit;
        StringBuilder sql = new StringBuilder(
                "SELECT id, company_id, legal_name, email, unsubscribe_token, marketing_status, marketing_consent " +
                        "FROM customers " +
                        "WHERE email IS NOT NULL AND active = true");

        if (filter.customerType() != null) {
            sql.append(" AND type = '").append(filter.customerType()).append("'");
        }

        sql.append(" LIMIT ").append(limit).append(" OFFSET ").append(offset);

        return jdbcClient.sql(sql.toString())
                .query((rs, rowNum) -> mapRow(rs)).list();
    }

    @Override
    public Optional<MarketingCustomer> findByToken(String token) {
        return jdbcClient.sql(
                "SELECT id, company_id, legal_name, email, unsubscribe_token, marketing_status, marketing_consent FROM customers WHERE unsubscribe_token = ?")
                .param(token)
                .query((rs, rowNum) -> mapRow(rs)).optional();
    }

    @Override
    public void updateMarketingStatus(String token, String status, Instant snoozedUntil) {
        jdbcClient.sql("UPDATE customers SET marketing_status = ?, updated_at = NOW() WHERE unsubscribe_token = ?")
                .param(status)
                .param(token)
                .update();
    }

    private MarketingCustomer mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        boolean consent = rs.getBoolean("marketing_consent");
        String status = rs.getString("marketing_status");
        boolean canReceive = consent && ("SUBSCRIBED".equals(status));

        return new MarketingCustomer(
                rs.getLong("id"),
                UUID.fromString(rs.getString("company_id")),
                rs.getString("email"),
                rs.getString("legal_name"),
                canReceive,
                rs.getString("unsubscribe_token"));
    }
}