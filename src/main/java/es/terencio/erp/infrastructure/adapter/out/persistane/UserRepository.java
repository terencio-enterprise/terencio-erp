package es.terencio.erp.infrastructure.adapter.out.persistence;

import es.terencio.erp.application.dto.UserDto;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class UserRepository {

    private final JdbcClient jdbcClient;

    public UserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<UserDto> findUsersByStore(UUID storeId) {
        String sql = """
            SELECT id, username, full_name, role, pin_hash, 
                   CASE WHEN is_active THEN 1 ELSE 0 END as is_active,
                   created_at, updated_at
            FROM users
            WHERE store_id = :storeId
        """;

        return jdbcClient.sql(sql)
                .param("storeId", storeId)
                .query(UserDto.class)
                .list();
    }
}