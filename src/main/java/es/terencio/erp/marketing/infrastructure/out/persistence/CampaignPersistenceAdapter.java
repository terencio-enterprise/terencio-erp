package es.terencio.erp.marketing.infrastructure.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.domain.model.CampaignLog;
import es.terencio.erp.marketing.domain.model.DeliveryStatus;
import es.terencio.erp.marketing.domain.model.MarketingCampaign;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;

@Repository
public class CampaignPersistenceAdapter implements CampaignRepositoryPort {

    private final JdbcClient jdbcClient;

    public CampaignPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    private final RowMapper<MarketingTemplate> templateRowMapper = (rs, rowNum) -> new MarketingTemplate(
        rs.getLong("id"),
        rs.getObject("company_id", UUID.class),
        rs.getString("name"), // using name as code since 'code' isn't in the schema
        rs.getString("name"),
        rs.getString("subject_template"),
        rs.getString("body_html"),
        rs.getBoolean("is_active"),
        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : Instant.now(),
        rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : Instant.now()
    );

    private final RowMapper<MarketingCampaign> campaignRowMapper = (rs, rowNum) -> new MarketingCampaign(
        rs.getLong("id"),
        rs.getObject("company_id", UUID.class),
        rs.getString("name"),
        rs.getLong("template_id"),
        rs.getString("status"),
        rs.getTimestamp("scheduled_at") != null ? rs.getTimestamp("scheduled_at").toInstant() : null,
        rs.getInt("metrics_total_recipients"),
        rs.getInt("metrics_sent"),
        rs.getInt("metrics_opened"),
        rs.getInt("metrics_clicked")
    );

    private final RowMapper<CampaignLog> logRowMapper = (rs, rowNum) -> new CampaignLog(
        rs.getLong("id"),
        rs.getObject("campaign_id") != null ? rs.getLong("campaign_id") : null,
        rs.getObject("company_id", UUID.class),
        rs.getLong("customer_id"),
        rs.getLong("template_id"),
        rs.getTimestamp("sent_at") != null ? rs.getTimestamp("sent_at").toInstant() : null,
        rs.getString("status") != null ? DeliveryStatus.valueOf(rs.getString("status")) : DeliveryStatus.PENDING,
        rs.getString("message_id"),
        rs.getString("error_message"),
        rs.getTimestamp("opened_at") != null ? rs.getTimestamp("opened_at").toInstant() : null,
        rs.getTimestamp("clicked_at") != null ? rs.getTimestamp("clicked_at").toInstant() : null
    );

    @Override
    public Optional<MarketingTemplate> findTemplateById(Long id) {
        return jdbcClient.sql("SELECT * FROM marketing_templates WHERE id = ?")
            .param(id).query(templateRowMapper).optional();
    }

    @Override
    public List<MarketingTemplate> findAllTemplates(UUID companyId, String search) {
        String sql = "SELECT * FROM marketing_templates WHERE company_id = ?";
        if (search != null && !search.isBlank()) {
            sql += " AND name ILIKE ?";
            return jdbcClient.sql(sql).param(companyId).param("%" + search + "%").query(templateRowMapper).list();
        }
        return jdbcClient.sql(sql).param(companyId).query(templateRowMapper).list();
    }

    @Override
    public MarketingTemplate saveTemplate(MarketingTemplate template) {
        if (template.getId() == null) {
            Long id = jdbcClient.sql("INSERT INTO marketing_templates (company_id, name, subject_template, body_html, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id")
                .params(template.getCompanyId(), template.getName(), template.getSubjectTemplate(), template.getBodyHtml(), template.isActive(), Instant.now(), Instant.now())
                .query(Long.class).single();
            template.setId(id);
            return template;
        } else {
            jdbcClient.sql("UPDATE marketing_templates SET name = ?, subject_template = ?, body_html = ?, is_active = ?, updated_at = ? WHERE id = ?")
                .params(template.getName(), template.getSubjectTemplate(), template.getBodyHtml(), template.isActive(), Instant.now(), template.getId())
                .update();
            return template;
        }
    }

    @Override
    public void deleteTemplate(Long id) {
        jdbcClient.sql("DELETE FROM marketing_templates WHERE id = ?").param(id).update();
    }

    @Override
    public Optional<MarketingCampaign> findCampaignById(Long id) {
        return jdbcClient.sql("SELECT * FROM marketing_campaigns WHERE id = ?")
            .param(id).query(campaignRowMapper).optional();
    }

    @Override
    public MarketingCampaign saveCampaign(MarketingCampaign campaign) {
        if (campaign.getId() == null) {
            Long id = jdbcClient.sql("INSERT INTO marketing_campaigns (company_id, name, template_id, status, scheduled_at, metrics_total_recipients, metrics_sent, metrics_opened, metrics_clicked) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id")
                .params(campaign.getCompanyId(), campaign.getName(), campaign.getTemplateId(), campaign.getStatus(), campaign.getScheduledAt(), campaign.getMetricsTotalRecipients(), campaign.getMetricsSent(), campaign.getMetricsOpened(), campaign.getMetricsClicked())
                .query(Long.class).single();
            campaign.setId(id);
            return campaign;
        } else {
            jdbcClient.sql("UPDATE marketing_campaigns SET status = ?, metrics_total_recipients = ?, metrics_sent = ?, metrics_opened = ?, metrics_clicked = ? WHERE id = ?")
                .params(campaign.getStatus(), campaign.getMetricsTotalRecipients(), campaign.getMetricsSent(), campaign.getMetricsOpened(), campaign.getMetricsClicked(), campaign.getId())
                .update();
            return campaign;
        }
    }

    @Override
    public void incrementCampaignMetric(Long campaignId, String metricType) {
        if (campaignId == null) return;
        String column = "";
        switch (metricType) {
            case "opened": column = "metrics_opened"; break;
            case "clicked": column = "metrics_clicked"; break;
            case "sent": column = "metrics_sent"; break;
            default: return;
        }
        jdbcClient.sql("UPDATE marketing_campaigns SET " + column + " = " + column + " + 1 WHERE id = ?")
            .param(campaignId).update();
    }

    @Override
    public CampaignLog saveLog(CampaignLog log) {
        if (log.getId() == null) {
            Long id = jdbcClient.sql("INSERT INTO marketing_email_logs (campaign_id, company_id, customer_id, template_id, sent_at, status, message_id, error_message, opened_at, clicked_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id")
                .params(log.getCampaignId(), log.getCompanyId(), log.getCustomerId(), log.getTemplateId(), log.getSentAt(), log.getStatus() != null ? log.getStatus().name() : DeliveryStatus.PENDING.name(), log.getMessageId(), log.getErrorMessage(), log.getOpenedAt(), log.getClickedAt())
                .query(Long.class).single();
            log.setId(id);
            return log;
        } else {
            jdbcClient.sql("UPDATE marketing_email_logs SET status = ?, message_id = ?, error_message = ?, opened_at = ?, clicked_at = ? WHERE id = ?")
                .params(log.getStatus() != null ? log.getStatus().name() : DeliveryStatus.PENDING.name(), log.getMessageId(), log.getErrorMessage(), log.getOpenedAt(), log.getClickedAt(), log.getId())
                .update();
            return log;
        }
    }

    @Override
    public Optional<CampaignLog> findLogById(Long id) {
        return jdbcClient.sql("SELECT * FROM marketing_email_logs WHERE id = ?")
            .param(id).query(logRowMapper).optional();
    }

    @Override
    public List<CampaignLog> findLogsByStatus(String status) {
        if (status == null || status.isBlank()) {
            return jdbcClient.sql("SELECT * FROM marketing_email_logs").query(logRowMapper).list();
        }
        return jdbcClient.sql("SELECT * FROM marketing_email_logs WHERE status = ?")
            .param(status).query(logRowMapper).list();
    }

    @Override
    public boolean hasLog(Long campaignId, Long customerId) {
        if (campaignId == null || customerId == null) return false;
        Integer count = jdbcClient.sql("SELECT count(*) FROM marketing_email_logs WHERE campaign_id = ? AND customer_id = ?")
            .params(campaignId, customerId).query(Integer.class).single();
        return count != null && count > 0;
    }
}
