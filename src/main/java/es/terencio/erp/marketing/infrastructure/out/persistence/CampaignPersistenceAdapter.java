package es.terencio.erp.marketing.infrastructure.out.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.domain.model.CampaignLog;
import es.terencio.erp.marketing.domain.model.CampaignStatus;
import es.terencio.erp.marketing.domain.model.DeliveryStatus;
import es.terencio.erp.marketing.domain.model.EmailDeliveryEvent;
import es.terencio.erp.marketing.domain.model.MarketingCampaign;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;

@Repository
public class CampaignPersistenceAdapter implements CampaignRepositoryPort {

    private final JdbcClient jdbcClient;

    public CampaignPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean acquireSchedulerLock(String lockName) {
        Boolean locked = jdbcClient.sql("SELECT pg_try_advisory_lock(hashtext(?)::integer)")
                .param(lockName)
                .query(Boolean.class)
                .single();
        return Boolean.TRUE.equals(locked);
    }

    @Override
    public void releaseSchedulerLock(String lockName) {
        jdbcClient.sql("SELECT pg_advisory_unlock(hashtext(?)::integer)")
                .param(lockName)
                .query(Boolean.class)
                .single();
    }

    private final RowMapper<MarketingCampaign> campaignRowMapper = (rs, rowNum) -> new MarketingCampaign(
            rs.getLong("id"),
            rs.getObject("company_id", UUID.class),
            rs.getString("name"),
            rs.getLong("template_id"),
            CampaignStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("scheduled_at") != null ? rs.getTimestamp("scheduled_at").toInstant() : null,
            rs.getInt("metrics_total_recipients"),
            rs.getInt("metrics_sent"),
            rs.getInt("metrics_opened"),
            rs.getInt("metrics_clicked"),
            rs.getInt("metrics_bounced")
    // TODO: (Improvement) Domain model is missing DB fields: metrics_delivered,
    // metrics_unsubscribed, segment_id, started_at, completed_at
    );

    private final RowMapper<CampaignLog> logRowMapper = (rs, rowNum) -> new CampaignLog(
            rs.getLong("id"),
            rs.getObject("campaign_id") != null ? rs.getLong("campaign_id") : null,
            rs.getObject("company_id", UUID.class),
            rs.getLong("customer_id"),
            rs.getLong("template_id"),
            rs.getTimestamp("sent_at") != null ? rs.getTimestamp("sent_at").toInstant() : null,
            rs.getTimestamp("delivered_at") != null ? rs.getTimestamp("delivered_at").toInstant() : null,
            rs.getString("status") != null ? DeliveryStatus.valueOf(rs.getString("status")) : DeliveryStatus.PENDING,
            rs.getString("message_id"),
            rs.getString("error_message"),
            rs.getTimestamp("opened_at") != null ? rs.getTimestamp("opened_at").toInstant() : null,
            rs.getTimestamp("clicked_at") != null ? rs.getTimestamp("clicked_at").toInstant() : null
    // TODO: (Improvement) Domain model is missing DB fields: bounced_at,
    // unsubscribed_at
    );

    private final RowMapper<MarketingTemplate> templateRowMapper = (rs, rowNum) -> new MarketingTemplate(
            rs.getLong("id"),
            rs.getObject("company_id", UUID.class),
            null, // TODO: (Improvement) DB schema is missing 'code' field present in the domain
                  // model
            rs.getString("name"),
            rs.getString("subject_template"),
            // TODO: (Improvement) Domain model is missing DB field: preheader_template
            rs.getString("body_html"),
            rs.getBoolean("is_active"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null,
            rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : null);

    @Override
    public Optional<MarketingTemplate> findTemplateById(Long id) {
        return jdbcClient.sql("SELECT * FROM marketing_templates WHERE id = ?")
                .param(id)
                .query(templateRowMapper)
                .optional();
    }

    @Override
    public List<MarketingTemplate> findAllTemplates(UUID companyId, String search) {
        String sql = "SELECT * FROM marketing_templates WHERE company_id = ?";
        if (search != null && !search.trim().isEmpty()) {
            sql += " AND (name ILIKE ? OR subject_template ILIKE ?)";
            return jdbcClient.sql(sql)
                    .param(companyId)
                    .param("%" + search + "%")
                    .param("%" + search + "%")
                    .query(templateRowMapper)
                    .list();
        }

        return jdbcClient.sql(sql)
                .param(companyId)
                .query(templateRowMapper)
                .list();
    }

    @Override
    public MarketingTemplate saveTemplate(MarketingTemplate template) {
        Instant now = Instant.now();
        // TODO: (Improvement) Add 'code' column mapping in INSERT/UPDATE if added to
        // schema
        if (template.getId() == null) {
            Long id = jdbcClient.sql(
                    "INSERT INTO marketing_templates (company_id, name, subject_template, body_html, is_active, created_at, updated_at) "
                            +
                            "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id")
                    .params(
                            template.getCompanyId(),
                            template.getName(),
                            template.getSubjectTemplate(),
                            template.getBodyHtml(),
                            template.isActive(),
                            template.getCreatedAt() != null ? Timestamp.from(template.getCreatedAt())
                                    : Timestamp.from(now),
                            template.getUpdatedAt() != null ? Timestamp.from(template.getUpdatedAt())
                                    : Timestamp.from(now))
                    .query(Long.class).single();
            template.setId(id);
            return template;
        } else {
            jdbcClient.sql(
                    "UPDATE marketing_templates SET name = ?, subject_template = ?, body_html = ?, is_active = ?, updated_at = ? WHERE id = ?")
                    .params(
                            template.getName(),
                            template.getSubjectTemplate(),
                            template.getBodyHtml(),
                            template.isActive(),
                            Timestamp.from(now),
                            template.getId())
                    .update();
            return template;
        }
    }

    @Override
    public void deleteTemplate(Long id) {
        jdbcClient.sql("DELETE FROM marketing_templates WHERE id = ?")
                .param(id)
                .update();
    }

    @Override
    public Optional<MarketingCampaign> findCampaignById(Long id) {
        return jdbcClient.sql("SELECT * FROM marketing_campaigns WHERE id = ?")
                .param(id)
                .query(campaignRowMapper)
                .optional();
    }

    @Override
    public List<MarketingCampaign> findScheduledCampaignsToLaunch(Instant now) {
        return jdbcClient.sql("SELECT * FROM marketing_campaigns WHERE status = 'SCHEDULED' AND scheduled_at <= ?")
                .param(Timestamp.from(now))
                .query(campaignRowMapper)
                .list();
    }

    @Override
    public MarketingCampaign saveCampaign(MarketingCampaign campaign) {
        if (campaign.getId() == null) {
            Long id = jdbcClient.sql(
                    "INSERT INTO marketing_campaigns (company_id, name, template_id, status, scheduled_at, metrics_total_recipients) "
                            +
                            "VALUES (?, ?, ?, ?, ?, ?) RETURNING id")
                    .params(
                            campaign.getCompanyId(),
                            campaign.getName(),
                            campaign.getTemplateId(),
                            campaign.getStatus().name(),
                            campaign.getScheduledAt() != null ? Timestamp.from(campaign.getScheduledAt()) : null,
                            campaign.getMetricsTotalRecipients())
                    .query(Long.class).single();
            campaign.setId(id);
            return campaign;
        } else {
            jdbcClient.sql(
                    "UPDATE marketing_campaigns SET status = ?, metrics_sent = ?, metrics_opened = ?, " +
                            "metrics_clicked = ?, metrics_bounced = ?, updated_at = NOW() WHERE id = ?")
                    .params(
                            campaign.getStatus().name(),
                            campaign.getMetricsSent(),
                            campaign.getMetricsOpened(),
                            campaign.getMetricsClicked(),
                            campaign.getMetricsBounced(),
                            campaign.getId())
                    .update();
            return campaign;
        }
    }

    @Override
    public void incrementCampaignMetric(Long campaignId, String metricType) {
        String col = switch (metricType.toLowerCase()) {
            case "opened" -> "metrics_opened";
            case "clicked" -> "metrics_clicked";
            case "sent" -> "metrics_sent";
            case "bounced" -> "metrics_bounced";
            case "delivered" -> "metrics_delivered";
            case "unsubscribed" -> "metrics_unsubscribed";
            default -> null;
        };

        if (col != null) {
            jdbcClient.sql("UPDATE marketing_campaigns SET " + col + " = " + col + " + 1 WHERE id = ?")
                    .param(campaignId)
                    .update();
        }
    }

    @Override
    public int countDailySendsForCompany(UUID companyId, Instant startOfDay) {
        Integer count = jdbcClient
                .sql("SELECT count(*) FROM marketing_email_logs WHERE company_id = ? AND sent_at >= ?")
                .params(companyId, Timestamp.from(startOfDay))
                .query(Integer.class)
                .single();
        return count != null ? count : 0;
    }

    @Override
    public CampaignLog saveLog(CampaignLog log) {
        if (log.getId() == null) {
            Long id = jdbcClient.sql(
                    "INSERT INTO marketing_email_logs (campaign_id, company_id, customer_id, template_id, sent_at, status, message_id) "
                            +
                            "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id")
                    .params(
                            log.getCampaignId(),
                            log.getCompanyId(),
                            log.getCustomerId(),
                            log.getTemplateId(),
                            log.getSentAt() != null ? Timestamp.from(log.getSentAt()) : null,
                            log.getStatus() != null ? log.getStatus().name() : DeliveryStatus.PENDING.name(),
                            log.getMessageId())
                    .query(Long.class).single();
            log.setId(id);
            return log;
        } else {
            jdbcClient.sql(
                    "UPDATE marketing_email_logs SET status = ?, message_id = ?, error_message = ?, " +
                            "opened_at = ?, clicked_at = ?, delivered_at = ? WHERE id = ?")
                    .params(
                            log.getStatus().name(),
                            log.getMessageId(),
                            log.getErrorMessage(),
                            log.getOpenedAt() != null ? Timestamp.from(log.getOpenedAt()) : null,
                            log.getClickedAt() != null ? Timestamp.from(log.getClickedAt()) : null,
                            log.getDeliveredAt() != null ? Timestamp.from(log.getDeliveredAt()) : null,
                            log.getId())
                    .update();
            return log;
        }
    }

    @Override
    public Optional<CampaignLog> findLogById(Long id) {
        return jdbcClient.sql("SELECT * FROM marketing_email_logs WHERE id = ?")
                .param(id)
                .query(logRowMapper)
                .optional();
    }

    @Override
    public Optional<CampaignLog> findLogByMessageId(String messageId) {
        return jdbcClient.sql("SELECT * FROM marketing_email_logs WHERE message_id = ?")
                .param(messageId)
                .query(logRowMapper)
                .optional();
    }

    @Override
    public boolean hasLog(Long campaignId, Long customerId) {
        Integer count = jdbcClient.sql(
                "SELECT count(*) FROM marketing_email_logs WHERE campaign_id = ? AND customer_id = ? AND status != 'FAILED'")
                .params(campaignId, customerId)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    @Override
    public void saveDeliveryEvent(EmailDeliveryEvent event) {
        jdbcClient.sql(
                "INSERT INTO email_delivery_events (provider_message_id, email_address, event_type, bounce_type, raw_payload, processed, created_at) "
                        +
                        "VALUES (?, ?, ?, ?, ?::jsonb, true, ?)")
                .params(
                        event.getProviderMessageId(),
                        event.getEmailAddress(),
                        event.getEventType(),
                        null, // TODO: (Faulting field) Pass event.getBounceType() once the field is added to
                              // the EmailDeliveryEvent domain
                        event.getRawPayload(),
                        Timestamp.from(Instant.now()))
                .update();
    }

    @Override
    public void markCustomerAsBouncedOrComplained(Long customerId, String status) {
        jdbcClient.sql("UPDATE customers SET marketing_status = ?, updated_at = NOW() WHERE id = ?")
                .params(status, customerId)
                .update();
    }
}