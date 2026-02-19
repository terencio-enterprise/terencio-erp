package es.terencio.erp.marketing.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.domain.model.Campaign;
import es.terencio.erp.marketing.domain.model.DeliveryStatus;
import es.terencio.erp.marketing.domain.model.MarketingAttachment;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;

@Repository
public class JdbcCampaignRepository implements CampaignRepositoryPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcCampaignRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<MarketingTemplate> findAllTemplates(UUID companyId, String search) {
        String sql = "SELECT * FROM marketing_templates WHERE company_id = :companyId AND (CAST(:search AS VARCHAR) IS NULL OR LOWER(name) LIKE LOWER(:searchPattern))";
        String searchPattern = search != null ? "%" + search + "%" : null;

        return jdbcTemplate.query(sql,
                new MapSqlParameterSource("search", search)
                        .addValue("searchPattern", searchPattern)
                        .addValue("companyId", companyId),
                new TemplateRowMapper());
    }

    @Override
    public Optional<MarketingTemplate> findTemplateById(Long id) {
        String sql = "SELECT * FROM marketing_templates WHERE id = :id";
        try {
            MarketingTemplate template = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("id", id),
                    new TemplateRowMapper());
            if (template != null) {
                template.setAttachments(findAttachmentsByTemplateId(id));
            }
            return Optional.ofNullable(template);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private List<MarketingAttachment> findAttachmentsByTemplateId(Long templateId) {
        String sql = "SELECT * FROM marketing_attachments WHERE template_id = :templateId";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("templateId", templateId),
                (rs, rowNum) -> new MarketingAttachment(
                        rs.getLong("id"),
                        rs.getLong("template_id"),
                        rs.getString("filename"),
                        rs.getString("content_type"),
                        rs.getLong("file_size_bytes"),
                        rs.getString("s3_bucket"),
                        rs.getString("s3_key"),
                        rs.getString("s3_region")));
    }

    @Override
    public MarketingTemplate saveTemplate(MarketingTemplate template) {
        if (template.getId() == null) {
            return insertTemplate(template);
        } else {
            return updateTemplate(template);
        }
    }

    private MarketingTemplate insertTemplate(MarketingTemplate t) {
        String sql = "INSERT INTO marketing_templates (company_id, code, name, subject_template, body_html, is_active, created_at, updated_at) "
                +
                "VALUES (:companyId, :code, :name, :subject, :body, :active, :created, :updated)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("companyId", t.getCompanyId())
                .addValue("code", t.getCode())
                .addValue("name", t.getName())
                .addValue("subject", t.getSubjectTemplate())
                .addValue("body", t.getBodyHtml())
                .addValue("active", t.isActive())
                .addValue("created", java.sql.Timestamp.from(t.getCreatedAt()))
                .addValue("updated", java.sql.Timestamp.from(t.getUpdatedAt()));

        jdbcTemplate.update(sql, params, keyHolder, new String[] { "id" });
        t.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());

        return t;
    }

    private MarketingTemplate updateTemplate(MarketingTemplate t) {
        String sql = "UPDATE marketing_templates SET name=:name, subject_template=:subject, body_html=:body, is_active=:active, updated_at=:updated "
                +
                "WHERE id=:id";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", t.getId())
                .addValue("name", t.getName())
                .addValue("subject", t.getSubjectTemplate())
                .addValue("body", t.getBodyHtml())
                .addValue("active", t.isActive())
                .addValue("updated", java.sql.Timestamp.from(t.getUpdatedAt()));

        jdbcTemplate.update(sql, params);
        return t;
    }

    @Override
    public void deleteTemplate(Long id) {
        jdbcTemplate.update("DELETE FROM marketing_templates WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    @Override
    public Campaign saveLog(Campaign log) {
        String sql = "INSERT INTO marketing_logs (company_id, customer_id, template_id, sent_at, status, message_id, error_message) "
                +
                "VALUES (:companyId, :customerId, :templateId, :sentAt, :status, :messageId, :errorMessage)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("companyId", log.getCompanyId())
                .addValue("customerId", log.getCustomerId())
                .addValue("templateId", log.getTemplateId())
                .addValue("sentAt", java.sql.Timestamp.from(log.getSentAt()))
                .addValue("status", log.getStatus().name())
                .addValue("messageId", log.getMessageId())
                .addValue("errorMessage", log.getErrorMessage());

        jdbcTemplate.update(sql, params, keyHolder, new String[] { "id" });
        log.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return log;
    }

    @Override
    public List<Campaign> findLogsByStatus(String status) {
        String sql = "SELECT * FROM marketing_logs WHERE status = :status";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("status", status),
                (rs, i) -> new Campaign(
                        rs.getLong("id"),
                        rs.getObject("company_id", UUID.class),
                        rs.getLong("customer_id"),
                        rs.getLong("template_id"),
                        rs.getTimestamp("sent_at").toInstant(),
                        DeliveryStatus.valueOf(rs.getString("status")),
                        rs.getString("message_id"),
                        rs.getString("error_message")));
    }

    private static class TemplateRowMapper implements RowMapper<MarketingTemplate> {
        @Override
        public MarketingTemplate mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MarketingTemplate(
                    rs.getLong("id"),
                    rs.getObject("company_id", UUID.class),
                    rs.getString("code"),
                    rs.getString("name"),
                    rs.getString("subject_template"),
                    rs.getString("body_html"),
                    rs.getBoolean("is_active"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant(),
                    null); // attachments loaded separately
        }
    }
}
