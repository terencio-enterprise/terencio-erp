package es.terencio.erp.marketing.infrastructure.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.terencio.erp.marketing.application.dto.campaign.CampaignAudienceMember;
import es.terencio.erp.marketing.application.dto.campaign.CampaignLogResponse;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.domain.model.AudienceFilter;
import es.terencio.erp.marketing.domain.model.CampaignLog;
import es.terencio.erp.marketing.domain.model.CampaignStatus;
import es.terencio.erp.marketing.domain.model.DeliveryStatus;
import es.terencio.erp.marketing.domain.model.EmailDeliveryEvent;
import es.terencio.erp.marketing.domain.model.MarketingCampaign;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;
import es.terencio.erp.shared.domain.query.PageResult;

@Repository
public class JdbcCampaignRepository implements CampaignRepositoryPort {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public JdbcCampaignRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    // ==========================================
    // ATOMIC CAMPAIGN OPERATIONS
    // ==========================================

    @Override
    public boolean tryStartCampaign(Long campaignId, boolean isRelaunch) {
        String sql;
        if (!isRelaunch) {
            sql = """
                UPDATE marketing_campaigns
                SET status = 'SENDING', started_at = NOW(), updated_at = NOW()
                WHERE id = :id AND status IN ('DRAFT', 'SCHEDULED')
            """;
        } else {
            sql = """
                UPDATE marketing_campaigns
                SET status = 'SENDING', updated_at = NOW()
                WHERE id = :id AND status IN ('COMPLETED', 'SENDING')
            """;
        }
        int updatedRows = jdbc.update(sql, new MapSqlParameterSource("id", campaignId));
        return updatedRows > 0;
    }

    @Override
    public void updateCampaignTotalRecipients(Long campaignId, int totalRecipients) {
        String sql = "UPDATE marketing_campaigns SET metrics_total_recipients = :total, updated_at = NOW() WHERE id = :id";
        jdbc.update(sql, new MapSqlParameterSource("total", totalRecipients).addValue("id", campaignId));
    }

    @Override
    public void completeCampaign(Long campaignId, int sentInThisSession) {
        String sql = """
            UPDATE marketing_campaigns
            SET status = 'COMPLETED', completed_at = NOW(), updated_at = NOW(),
                metrics_sent = metrics_sent + :sent
            WHERE id = :id
        """;
        jdbc.update(sql, new MapSqlParameterSource("sent", sentInThisSession).addValue("id", campaignId));
    }

    // ==========================================
    // CAMPAIGNS
    // ==========================================

    @Override
    public MarketingCampaign saveCampaign(MarketingCampaign campaign) {
        if (campaign.getId() == null) {
            String sql = """
                INSERT INTO marketing_campaigns (
                    company_id, name, template_id, status, scheduled_at, started_at, completed_at,
                    metrics_total_recipients, metrics_sent, metrics_delivered, metrics_opened,
                    metrics_clicked, metrics_bounced, metrics_unsubscribed, audience_filter, 
                    created_at, updated_at
                ) VALUES (
                    :companyId, :name, :templateId, :status, :scheduledAt, :startedAt, :completedAt,
                    :recipients, :sent, :delivered, :opened, :clicked, :bounced, :unsubscribed, 
                    :audienceFilter::jsonb, :createdAt, :updatedAt
                ) RETURNING id
            """;
            
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(sql, mapCampaignParams(campaign), keyHolder, new String[]{"id"});
            
            try {
                Number key = keyHolder.getKey();
                if (key != null) {
                    var idField = MarketingCampaign.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(campaign, key.longValue());
                } else {
                    throw new RuntimeException("Failed to generate campaign ID");
                }
            } catch (Exception e) {
                throw new RuntimeException("Error mapping campaign ID", e);
            }
        } else {
            String sql = """
                UPDATE marketing_campaigns SET
                    status = :status, scheduled_at = :scheduledAt, started_at = :startedAt, completed_at = :completedAt,
                    metrics_total_recipients = :recipients, metrics_sent = :sent, metrics_delivered = :delivered,
                    metrics_opened = :opened, metrics_clicked = :clicked, metrics_bounced = :bounced, 
                    metrics_unsubscribed = :unsubscribed, audience_filter = :audienceFilter::jsonb, updated_at = :updatedAt
                WHERE id = :id
            """;
            jdbc.update(sql, mapCampaignParams(campaign));
        }
        return campaign;
    }

    @Override
    public PageResult<MarketingCampaign> findCampaigns(UUID companyId, String search, String status, int page, int size) {
        int safeSize = Math.max(size, 1);
        int offset = page * safeSize;

        StringBuilder sql = new StringBuilder("""
            SELECT *, COUNT(*) OVER() as total_elements 
            FROM marketing_campaigns 
            WHERE company_id = :companyId
        """);
        MapSqlParameterSource params = new MapSqlParameterSource("companyId", companyId);

        if (search != null && !search.isBlank()) {
            sql.append(" AND name ILIKE :search");
            params.addValue("search", "%" + search + "%");
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = :status");
            params.addValue("status", status);
        }
        sql.append(" ORDER BY created_at DESC LIMIT :limit OFFSET :offset");
        params.addValue("limit", safeSize).addValue("offset", offset);

        record RowWithTotal(MarketingCampaign c, long total) {}
        List<RowWithTotal> rows = jdbc.query(sql.toString(), params, (rs, rowNum) -> 
            new RowWithTotal(mapRowToCampaign(rs, rowNum), rs.getLong("total_elements")));
            
        long totalElements = rows.isEmpty() ? 0 : rows.get(0).total();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        return new PageResult<>(rows.stream().map(RowWithTotal::c).toList(), totalElements, totalPages, page, safeSize);
    }

    @Override
    public Optional<MarketingCampaign> findCampaignById(Long id) {
        String sql = "SELECT * FROM marketing_campaigns WHERE id = :id";
        List<MarketingCampaign> list = jdbc.query(sql, new MapSqlParameterSource("id", id), this::mapRowToCampaign);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<MarketingCampaign> findScheduledCampaignsToLaunch(Instant now) {
        String sql = """
            SELECT * FROM marketing_campaigns 
            WHERE status = 'SCHEDULED' AND scheduled_at <= :now
        """;
        return jdbc.query(sql, new MapSqlParameterSource("now", java.sql.Timestamp.from(now)), this::mapRowToCampaign);
    }

    @Override
    public void incrementCampaignMetric(Long campaignId, String metric) {
        String columnName = switch (metric.toLowerCase()) {
            case "sent" -> "metrics_sent";
            case "delivered" -> "metrics_delivered";
            case "opened" -> "metrics_opened";
            case "clicked" -> "metrics_clicked";
            case "bounced" -> "metrics_bounced";
            case "unsubscribed" -> "metrics_unsubscribed";
            default -> throw new IllegalArgumentException("Invalid metric name: " + metric);
        };
        
        String sql = "UPDATE marketing_campaigns SET " + columnName + " = " + columnName + " + 1 WHERE id = :id";
        jdbc.update(sql, new MapSqlParameterSource("id", campaignId));
    }

    // ==========================================
    // TEMPLATES
    // ==========================================

    @Override
    public MarketingTemplate saveTemplate(MarketingTemplate template) {
        if (template.getId() == null) {
            String sql = """
                INSERT INTO marketing_templates (company_id, name, subject_template, body_html, is_active, created_at, updated_at)
                VALUES (:companyId, :name, :subjectTemplate, :bodyHtml, :isActive, :createdAt, :updatedAt)
                RETURNING id
            """;
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(sql, mapTemplateParams(template), keyHolder, new String[]{"id"});
            
            try {
                Number key = keyHolder.getKey();
                if (key != null) {
                    var idField = MarketingTemplate.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(template, key.longValue());
                } else {
                    throw new RuntimeException("Failed to generate template ID");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            String sql = """
                UPDATE marketing_templates SET 
                    name = :name, subject_template = :subjectTemplate, body_html = :bodyHtml, 
                    is_active = :isActive, updated_at = :updatedAt 
                WHERE id = :id
            """;
            jdbc.update(sql, mapTemplateParams(template));
        }
        return template;
    }

    @Override
    public Optional<MarketingTemplate> findTemplateById(Long templateId) {
        String sql = "SELECT * FROM marketing_templates WHERE id = :id";
        List<MarketingTemplate> list = jdbc.query(sql, new MapSqlParameterSource("id", templateId), this::mapRowToTemplate);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<MarketingTemplate> findAllTemplates(UUID companyId, String search) {
        StringBuilder sql = new StringBuilder("SELECT * FROM marketing_templates WHERE company_id = :companyId");
        MapSqlParameterSource params = new MapSqlParameterSource("companyId", companyId);
        
        if (search != null && !search.isBlank()) {
            sql.append(" AND name ILIKE :search");
            params.addValue("search", "%" + search + "%");
        }
        sql.append(" ORDER BY id DESC");
        
        return jdbc.query(sql.toString(), params, this::mapRowToTemplate);
    }

    @Override
    public void deleteTemplate(Long id) {
        jdbc.update("DELETE FROM marketing_templates WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    // ==========================================
    // CAMPAIGN LOGS
    // ==========================================

    @Override
    public void saveLog(CampaignLog log) {
        if (log.getId() == null) {
            String sql = """
                INSERT INTO marketing_email_logs (
                    company_id, customer_id, template_id, campaign_id, message_id, 
                    status, error_message, sent_at, created_at
                ) VALUES (
                    :companyId, :customerId, :templateId, :campaignId, :messageId, 
                    :status, :errorMessage, :sentAt, NOW()
                ) RETURNING id
            """;
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(sql, mapLogParams(log), keyHolder, new String[]{"id"});
            
            try {
                Number key = keyHolder.getKey();
                if (key != null) {
                    var idField = CampaignLog.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(log, key.longValue());
                } else {
                    throw new RuntimeException("Failed to generate campaign log ID");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            String sql = """
                UPDATE marketing_email_logs SET 
                    status = :status, message_id = :messageId, error_message = :errorMessage,
                    sent_at = :sentAt, delivered_at = :deliveredAt, opened_at = :openedAt, 
                    clicked_at = :clickedAt, bounced_at = :bouncedAt, unsubscribed_at = :unsubscribedAt,
                    complained_at = :complainedAt
                WHERE id = :id
            """;
            jdbc.update(sql, mapLogParams(log));
        }
    }

    @Override
    public Optional<CampaignLog> findLogById(Long logId) {
        String sql = "SELECT * FROM marketing_email_logs WHERE id = :id";
        List<CampaignLog> list = jdbc.query(sql, new MapSqlParameterSource("id", logId), this::mapRowToLog);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<CampaignLog> findLogByMessageId(String messageId) {
        String sql = "SELECT * FROM marketing_email_logs WHERE message_id = :messageId";
        List<CampaignLog> list = jdbc.query(sql, new MapSqlParameterSource("messageId", messageId), this::mapRowToLog);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public PageResult<CampaignLogResponse> findCampaignLogs(UUID companyId, Long campaignId, String status, int page, int size) {
        int safeSize = Math.max(size, 1);
        int offset = page * safeSize;

        StringBuilder sql = new StringBuilder("""
            SELECT 
                l.*, 
                c.legal_name as customer_name, 
                c.email as customer_email
            FROM marketing_email_logs l
            JOIN customers c ON l.customer_id = c.id
            WHERE l.company_id = :companyId AND l.campaign_id = :campaignId
        """);
        MapSqlParameterSource params = new MapSqlParameterSource("companyId", companyId).addValue("campaignId", campaignId);

        if (status != null && !status.isBlank()) {
            sql.append(" AND l.status = :status");
            params.addValue("status", status);
        }
        sql.append(" ORDER BY l.created_at DESC LIMIT :limit OFFSET :offset");
        params.addValue("limit", safeSize).addValue("offset", offset);

        List<CampaignLogResponse> logs = jdbc.query(sql.toString(), params, (rs, rowNum) -> new CampaignLogResponse(
                rs.getLong("id"),
                rs.getLong("customer_id"),
                rs.getString("customer_name"),
                rs.getString("customer_email"),
                DeliveryStatus.valueOf(rs.getString("status")),
                rs.getString("error_message"),
                getInstant(rs, "sent_at"),
                getInstant(rs, "delivered_at"),
                getInstant(rs, "opened_at"),
                getInstant(rs, "clicked_at"),
                getInstant(rs, "bounced_at"),
                getInstant(rs, "unsubscribed_at"),
                getInstant(rs, "complained_at")
        ));

        String countSql = "SELECT COUNT(*) FROM marketing_email_logs l WHERE company_id = :companyId AND campaign_id = :campaignId";
        if (status != null && !status.isBlank()) countSql += " AND status = :status";
        long total = jdbc.queryForObject(countSql, params, Long.class);

        int totalPages = (int) Math.ceil((double) total / safeSize);
        return new PageResult<>(logs, total, totalPages, page, safeSize);
    }

    private Instant getInstant(ResultSet rs, String col) throws SQLException {
        java.sql.Timestamp ts = rs.getTimestamp(col);
        return ts != null ? ts.toInstant() : null;
    }

    // ==========================================
    // DELIVERY EVENTS & CRM STATUS
    // ==========================================

    @Override
    public void saveDeliveryEvent(EmailDeliveryEvent event) {
        String sql = """
            INSERT INTO email_delivery_events (
                provider_message_id, email_address, event_type, bounce_type, raw_payload, created_at
            ) VALUES (
                :messageId, :email, :eventType, :bounceType, :payload::jsonb, :createdAt
            )
        """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("messageId", event.getProviderMessageId())
            .addValue("email", event.getEmailAddress())
            .addValue("eventType", event.getEventType())
            .addValue("bounceType", event.getBounceType())
            .addValue("payload", event.getRawPayload())
            .addValue("createdAt", java.sql.Timestamp.from(event.getCreatedAt()));
        jdbc.update(sql, params);
    }

    @Override
    public void markCustomerAsBouncedOrComplained(Long customerId, String status) {
        String sql = "UPDATE customers SET marketing_status = :status WHERE id = :id";
        jdbc.update(sql, new MapSqlParameterSource().addValue("status", status).addValue("id", customerId));
    }

    @Override
    public PageResult<CampaignAudienceMember> findCampaignAudience(UUID companyId, Long campaignId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 500);
        int safePage = Math.max(page, 0);
        int offset = safePage * safeSize;

        String sql = """
            SELECT
                c.id                  AS customer_id,
                c.email               AS email,
                c.legal_name          AS name,
                c.marketing_status    AS marketing_status,
                COALESCE(cl.status, 'NOT_SENT') AS send_status,
                COUNT(*) OVER()       AS total_elements
            FROM marketing_campaigns mc
            JOIN customers c
            ON c.company_id = mc.company_id
            LEFT JOIN marketing_segments ms
            ON mc.segment_id = ms.id
            LEFT JOIN LATERAL (
                SELECT cl.status
                FROM marketing_email_logs cl
                WHERE cl.campaign_id = mc.id
                AND cl.customer_id = c.id
                ORDER BY cl.id DESC
                LIMIT 1
            ) cl ON true
            WHERE mc.id = :campaignId
            AND mc.company_id = :companyId
            AND c.email IS NOT NULL
            AND c.active = true
            AND c.deleted_at IS NULL
            AND (
                    mc.segment_id IS NULL
                    OR (
                        (ms.filter_types IS NULL OR c.type = ANY(ms.filter_types))
                        AND (ms.filter_tags IS NULL OR c.tags && ms.filter_tags)
                        AND (ms.filter_city IS NULL OR c.city = ms.filter_city)
                        AND (ms.filter_origin IS NULL OR c.origin = ms.filter_origin)
                        AND (ms.filter_marketing_status IS NULL OR c.marketing_status = ms.filter_marketing_status)
                        AND (ms.filter_registered_after IS NULL OR c.created_at >= ms.filter_registered_after)
                        AND (ms.filter_registered_before IS NULL OR c.created_at <= ms.filter_registered_before)
                    )
            )
            ORDER BY c.created_at DESC, c.id DESC
            LIMIT :limit OFFSET :offset
            """;

        record Row(CampaignAudienceMember member, long total) {}

        List<Row> rows = jdbc.query(sql, new MapSqlParameterSource()
                .addValue("campaignId", campaignId)
                .addValue("companyId", companyId)
                .addValue("limit", safeSize)
                .addValue("offset", offset),
                (rs, rowNum) -> new Row(
                        new CampaignAudienceMember(
                                rs.getLong("customer_id"),
                                rs.getString("email"),
                                rs.getString("name"),
                                rs.getString("marketing_status"),
                                rs.getString("send_status")
                        ),
                        rs.getLong("total_elements")
                )); 

        long totalElements = rows.isEmpty() ? 0 : rows.get(0).total();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);

        return new PageResult<>(
                rows.stream().map(Row::member).toList(),
                totalElements,
                totalPages,
                safePage,
                safeSize
        );
    }

    // ==========================================
    // DISTRIBUTED SCHEDULER LOCKING
    // ==========================================

    @Override
    public boolean acquireSchedulerLock(String lockName) {
        String sql = "SELECT pg_try_advisory_lock(hashtext(:lockName))";
        Boolean locked = jdbc.queryForObject(sql, new MapSqlParameterSource("lockName", lockName), Boolean.class);
        return Boolean.TRUE.equals(locked);
    }

    @Override
    public void releaseSchedulerLock(String lockName) {
        String sql = "SELECT pg_advisory_unlock(hashtext(:lockName))";
        jdbc.queryForObject(sql, new MapSqlParameterSource("lockName", lockName), Boolean.class);
    }

    // ==========================================
    // MAPPER UTILS
    // ==========================================

    private MapSqlParameterSource mapCampaignParams(MarketingCampaign c) {
        return new MapSqlParameterSource()
            .addValue("id", c.getId())
            .addValue("companyId", c.getCompanyId())
            .addValue("name", c.getName())
            .addValue("templateId", c.getTemplateId())
            .addValue("status", c.getStatus().name())
            .addValue("scheduledAt", c.getScheduledAt() != null ? java.sql.Timestamp.from(c.getScheduledAt()) : null)
            .addValue("startedAt", c.getStartedAt() != null ? java.sql.Timestamp.from(c.getStartedAt()) : null)
            .addValue("completedAt", c.getCompletedAt() != null ? java.sql.Timestamp.from(c.getCompletedAt()) : null)
            .addValue("recipients", c.getTotalRecipients())
            .addValue("sent", c.getSent())
            .addValue("delivered", c.getDelivered())
            .addValue("opened", c.getOpened())
            .addValue("clicked", c.getClicked())
            .addValue("bounced", c.getBounced())
            .addValue("unsubscribed", c.getUnsubscribed())
            .addValue("audienceFilter", serializeJson(c.getAudienceFilter()))
            .addValue("createdAt", java.sql.Timestamp.from(c.getCreatedAt()))
            .addValue("updatedAt", java.sql.Timestamp.from(c.getUpdatedAt()));
    }

    private MapSqlParameterSource mapTemplateParams(MarketingTemplate t) {
        return new MapSqlParameterSource()
            .addValue("id", t.getId())
            .addValue("companyId", t.getCompanyId())
            .addValue("name", t.getName())
            .addValue("subjectTemplate", t.getSubjectTemplate())
            .addValue("bodyHtml", t.getBodyHtml())
            .addValue("isActive", t.isActive())
            .addValue("createdAt", java.sql.Timestamp.from(t.getCreatedAt()))
            .addValue("updatedAt", java.sql.Timestamp.from(t.getUpdatedAt()));
    }

    private MapSqlParameterSource mapLogParams(CampaignLog l) {
        return new MapSqlParameterSource()
            .addValue("id", l.getId())
            .addValue("companyId", l.getCompanyId())
            .addValue("customerId", l.getCustomerId())
            .addValue("templateId", l.getTemplateId())
            .addValue("campaignId", l.getCampaignId())
            .addValue("status", l.getStatus().name())
            .addValue("messageId", l.getMessageId())
            .addValue("errorMessage", l.getErrorMessage())
            .addValue("sentAt", l.getSentAt() != null ? java.sql.Timestamp.from(l.getSentAt()) : null)
            .addValue("deliveredAt", l.getDeliveredAt() != null ? java.sql.Timestamp.from(l.getDeliveredAt()) : null)
            .addValue("openedAt", l.getOpenedAt() != null ? java.sql.Timestamp.from(l.getOpenedAt()) : null)
            .addValue("clickedAt", l.getClickedAt() != null ? java.sql.Timestamp.from(l.getClickedAt()) : null)
            .addValue("bouncedAt", l.getBouncedAt() != null ? java.sql.Timestamp.from(l.getBouncedAt()) : null)
            .addValue("unsubscribedAt", l.getUnsubscribedAt() != null ? java.sql.Timestamp.from(l.getUnsubscribedAt()) : null)
            .addValue("complainedAt", l.getComplainedAt() != null ? java.sql.Timestamp.from(l.getComplainedAt()) : null);
    }

    private MarketingCampaign mapRowToCampaign(ResultSet rs, int rowNum) throws SQLException {
        try {
            var constructor = MarketingCampaign.class.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            return (MarketingCampaign) constructor.newInstance(
                rs.getLong("id"),
                rs.getObject("company_id", UUID.class),
                rs.getString("name"),
                rs.getLong("template_id"),
                deserializeJson(rs.getString("audience_filter"), AudienceFilter.class),
                CampaignStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("scheduled_at") != null ? rs.getTimestamp("scheduled_at").toInstant() : null,
                rs.getTimestamp("started_at") != null ? rs.getTimestamp("started_at").toInstant() : null,
                rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toInstant() : null,
                rs.getInt("metrics_total_recipients"),
                rs.getInt("metrics_sent"),
                rs.getInt("metrics_delivered"),
                rs.getInt("metrics_opened"),
                rs.getInt("metrics_clicked"),
                rs.getInt("metrics_bounced"),
                rs.getInt("metrics_unsubscribed"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
            );
        } catch (Exception e) {
            throw new SQLException("Failed to instantiate MarketingCampaign via reflection", e);
        }
    }

    private MarketingTemplate mapRowToTemplate(ResultSet rs, int rowNum) throws SQLException {
        return new MarketingTemplate(
            rs.getLong("id"),
            rs.getObject("company_id", UUID.class),
            "", // Template code omitted in current SQL schema
            rs.getString("name"),
            rs.getString("subject_template"),
            rs.getString("body_html"),
            rs.getBoolean("is_active"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null,
            rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : null
        );
    }

    private CampaignLog mapRowToLog(ResultSet rs, int rowNum) throws SQLException {
        try {
            CampaignLog log = CampaignLog.createPending(
                rs.getLong("campaign_id"),
                rs.getObject("company_id", UUID.class),
                rs.getLong("customer_id"),
                rs.getLong("template_id")
            );

            // Rehydrate fields
            var idField = CampaignLog.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(log, rs.getLong("id"));

            var statusField = CampaignLog.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(log, DeliveryStatus.valueOf(rs.getString("status")));

            var messageIdField = CampaignLog.class.getDeclaredField("messageId");
            messageIdField.setAccessible(true);
            messageIdField.set(log, rs.getString("message_id"));

            var errorMessageField = CampaignLog.class.getDeclaredField("errorMessage");
            errorMessageField.setAccessible(true);
            errorMessageField.set(log, rs.getString("error_message"));

            // Timestamps
            setTimeField(log, "sentAt", rs, "sent_at");
            setTimeField(log, "deliveredAt", rs, "delivered_at");
            setTimeField(log, "openedAt", rs, "opened_at");
            setTimeField(log, "clickedAt", rs, "clicked_at");
            setTimeField(log, "bouncedAt", rs, "bounced_at");
            setTimeField(log, "unsubscribedAt", rs, "unsubscribed_at");
            setTimeField(log, "complainedAt", rs, "complained_at");

            return log;
        } catch (Exception e) {
            throw new SQLException("Failed to instantiate CampaignLog", e);
        }
    }

    private void setTimeField(CampaignLog log, String fieldName, ResultSet rs, String colName) throws Exception {
        var field = CampaignLog.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        var ts = rs.getTimestamp(colName);
        if (ts != null) {
            field.set(log, ts.toInstant());
        }
    }

    private String serializeJson(Object obj) {
        if (obj == null) return null;
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    private <T> T deserializeJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) return null;
        try {
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize from JSON", e);
        }
    }
}