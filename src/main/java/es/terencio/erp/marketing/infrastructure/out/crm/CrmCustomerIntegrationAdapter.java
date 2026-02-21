package es.terencio.erp.marketing.infrastructure.out.crm;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import es.terencio.erp.marketing.application.port.out.CustomerIntegrationPort;

@Component
public class CrmCustomerIntegrationAdapter implements CustomerIntegrationPort {

    private final JdbcClient jdbcClient;

    public CrmCustomerIntegrationAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<MarketingCustomer> findAudience(Long campaignId, int limit, int page) {

        int safeLimit = Math.min(Math.max(limit, 1), 500);
        int safePage = Math.max(page, 0);
        int offset = safePage * safeLimit;

        String sql = """
            SELECT c.id,
                c.company_id,
                c.legal_name,
                c.email,
                c.unsubscribe_token,
                c.marketing_status,
                c.marketing_consent
            FROM marketing_campaigns mc
            JOIN customers c ON c.company_id = mc.company_id
            LEFT JOIN marketing_segments ms ON mc.segment_id = ms.id
            WHERE mc.id = :campaignId
            AND c.email IS NOT NULL
            AND c.active = true
            AND c.deleted_at IS NULL
            
            -- If campaign has no segment → all customers
            AND (
                    mc.segment_id IS NULL
                    
                    OR
                    (
                        -- Segment filters
                        (ms.filter_types IS NULL OR c.type = ANY(ms.filter_types))
                        
                        AND
                        (ms.filter_tags IS NULL OR c.tags && ms.filter_tags)
                        
                        AND
                        (ms.filter_city IS NULL OR c.city = ms.filter_city)
                        
                        AND
                        (ms.filter_origin IS NULL OR c.origin = ms.filter_origin)
                        
                        AND
                        (ms.filter_marketing_status IS NULL 
                            OR c.marketing_status = ms.filter_marketing_status)
                        
                        AND
                        (ms.filter_registered_after IS NULL 
                            OR c.created_at >= ms.filter_registered_after)
                        
                        AND
                        (ms.filter_registered_before IS NULL 
                            OR c.created_at <= ms.filter_registered_before)
                    )
            )
            ORDER BY c.created_at DESC
            LIMIT :limit OFFSET :offset
            """;

        return jdbcClient.sql(sql)
                .param("campaignId", campaignId)
                .param("limit", safeLimit)
                .param("offset", offset)
                .query((rs, rowNum) -> mapRow(rs))
                .list();
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