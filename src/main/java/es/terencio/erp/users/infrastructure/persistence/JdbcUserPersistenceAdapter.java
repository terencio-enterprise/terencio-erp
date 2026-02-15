package es.terencio.erp.users.infrastructure.persistence;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.terencio.erp.users.application.dto.UserDto;
import es.terencio.erp.users.application.port.out.UserPort;

@Repository
public class JdbcUserPersistenceAdapter implements UserPort {

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public JdbcUserPersistenceAdapter(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<UserDto> findAll() {
        return jdbcClient.sql("SELECT * FROM users ORDER BY username")
                .query((rs, rowNum) -> mapRow(rs)).list();
    }

    @Override
    public Optional<UserDto> findById(Long id) {
        return jdbcClient.sql("SELECT * FROM users WHERE id = :id")
                .param("id", id).query((rs, rowNum) -> mapRow(rs)).optional();
    }

    @Override
    public Optional<UserDto> findByUsername(String username) {
        return jdbcClient.sql("SELECT * FROM users WHERE username = :username")
                .param("username", username).query((rs, rowNum) -> mapRow(rs)).optional();
    }

    private UserDto mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new UserDto(
                rs.getLong("id"), rs.getString("username"), rs.getString("full_name"),
                rs.getString("role"), rs.getBoolean("is_active") ? 1 : 0,
                parsePermissions(rs.getString("permissions_json")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    @Override
    public Long save(String username, String fullName, String role, String pinHash, String passwordHash, UUID companyId,
            UUID storeId, String permissionsJson) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient
                .sql("""
                        INSERT INTO users (username, full_name, role, pin_hash, password_hash, company_id, store_id, permissions_json, is_active, created_at, updated_at)
                        VALUES (:username, :fullName, :role, :pinHash, :passwordHash, :companyId, :storeId, CAST(:permissionsJson AS JSONB), TRUE, NOW(), NOW())
                        RETURNING id
                        """)
                .param("username", username).param("fullName", fullName).param("role", role)
                .param("pinHash", pinHash).param("passwordHash", passwordHash)
                .param("companyId", companyId).param("storeId", storeId)
                .param("permissionsJson", permissionsJson).update(keyHolder);
        return (Long) keyHolder.getKeys().get("id");
    }

    @Override
    public void update(Long id, String fullName, String role, UUID storeId, boolean isActive, String permissionsJson) {
        jdbcClient.sql("""
                UPDATE users SET full_name = :fullName, role = :role, store_id = :storeId,
                    is_active = :isActive, permissions_json = CAST(:permissionsJson AS JSONB), updated_at = NOW()
                WHERE id = :id
                """)
                .param("id", id).param("fullName", fullName).param("role", role)
                .param("storeId", storeId).param("isActive", isActive).param("permissionsJson", permissionsJson)
                .update();
    }

    @Override
    public void updatePin(Long id, String newPinHash) {
        jdbcClient.sql("UPDATE users SET pin_hash = :pinHash, updated_at = NOW() WHERE id = :id")
                .param("id", id).param("pinHash", newPinHash).update();
    }

    @Override
    public void updatePassword(Long id, String newPasswordHash) {
        jdbcClient.sql("UPDATE users SET password_hash = :passwordHash, updated_at = NOW() WHERE id = :id")
                .param("id", id).param("passwordHash", newPasswordHash).update();
    }

    private List<String> parsePermissions(String json) {
        if (json == null)
            return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}