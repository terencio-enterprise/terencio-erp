package es.terencio.erp.marketing.infrastructure.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import es.terencio.erp.marketing.application.port.out.MarketingSettingsRepositoryPort;
import es.terencio.erp.marketing.domain.model.CompanyMarketingSettings;

@Repository
public class JdbcMarketingSettingsRepository implements MarketingSettingsRepositoryPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMarketingSettingsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CompanyMarketingSettings> findByCompanyId(UUID companyId) {
        String sql = "SELECT * FROM company_marketing_settings WHERE company_id = ?";
        try {
            CompanyMarketingSettings settings = jdbcTemplate.queryForObject(sql, this::mapRow, companyId);
            return Optional.ofNullable(settings);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public CompanyMarketingSettings save(CompanyMarketingSettings settings) {
        String sql = """
            INSERT INTO company_marketing_settings (
                company_id, sender_name, sender_email, domain_verified, daily_send_limit, 
                welcome_email_active, welcome_template_id, welcome_delay_minutes, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (company_id) DO UPDATE SET
                sender_name = EXCLUDED.sender_name,
                sender_email = EXCLUDED.sender_email,
                domain_verified = EXCLUDED.domain_verified,
                daily_send_limit = EXCLUDED.daily_send_limit,
                welcome_email_active = EXCLUDED.welcome_email_active,
                welcome_template_id = EXCLUDED.welcome_template_id,
                welcome_delay_minutes = EXCLUDED.welcome_delay_minutes,
                updated_at = EXCLUDED.updated_at
            """;
            
        jdbcTemplate.update(sql,
            settings.getCompanyId(),
            settings.getSenderName(),
            settings.getSenderEmail(),
            settings.isDomainVerified(),
            settings.getDailySendLimit(),
            settings.isWelcomeEmailActive(),
            settings.getWelcomeTemplateId(),
            settings.getWelcomeDelayMinutes(),
            java.sql.Timestamp.from(settings.getUpdatedAt())
        );
        
        return settings;
    }

    private CompanyMarketingSettings mapRow(ResultSet rs, int rowNum) throws SQLException {
        Instant updatedAt = rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : null;
        Long welcomeTemplateId = rs.getObject("welcome_template_id", Long.class);
        
        return new CompanyMarketingSettings(
            rs.getObject("company_id", UUID.class),
            rs.getString("sender_name"),
            rs.getString("sender_email"),
            rs.getBoolean("domain_verified"),
            rs.getInt("daily_send_limit"),
            rs.getBoolean("welcome_email_active"),
            welcomeTemplateId,
            rs.getInt("welcome_delay_minutes"),
            updatedAt
        );
    }
}