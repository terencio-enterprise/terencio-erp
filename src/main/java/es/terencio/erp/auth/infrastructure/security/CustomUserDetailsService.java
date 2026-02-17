package es.terencio.erp.auth.infrastructure.security;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final JdbcClient jdbcClient;

    public CustomUserDetailsService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String sql = """
                    SELECT id, uuid, username, full_name, password_hash, role, store_id, company_id
                    FROM users
                    WHERE username = :username AND is_active = TRUE
                """;

        return jdbcClient.sql(sql)
                .param("username", username)
                .query((rs, rowNum) -> new CustomUserDetails(
                        rs.getLong("id"),
                        rs.getObject("uuid", UUID.class),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("password_hash"), // Backoffice password
                        rs.getString("role"),
                        rs.getObject("store_id", UUID.class),
                        rs.getObject("company_id", UUID.class)))
                .optional()
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}