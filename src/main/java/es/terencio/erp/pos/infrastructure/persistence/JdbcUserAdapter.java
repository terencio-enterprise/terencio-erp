package es.terencio.erp.pos.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.terencio.erp.pos.application.port.out.LoadUsersPort;
import es.terencio.erp.users.application.dto.UserDto;

/**
 * JDBC adapter implementing the LoadUsersPort.
 * This is the infrastructure implementation of the output port.
 */
@Repository
public class JdbcUserAdapter implements LoadUsersPort {

    private final JdbcClient jdbcClient;

    public JdbcUserAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<UserDto> loadByStore(UUID storeId) {
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
