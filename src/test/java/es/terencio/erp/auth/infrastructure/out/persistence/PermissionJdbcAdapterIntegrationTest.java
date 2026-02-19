package es.terencio.erp.auth.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.AbstractIntegrationTest;
import es.terencio.erp.auth.domain.model.AccessScope;

@Transactional
public class PermissionJdbcAdapterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PermissionJdbcAdapter permissionAdapter;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testPermission_WhenGrantExistsForCompany_ShouldReturnTrue() {
        jdbcTemplate.update(
                "INSERT INTO employee_access_grants (employee_id, role, scope, target_id, created_at) VALUES (?, ?, ?, ?, ?)",
                globalAdminId, "MANAGER", "COMPANY", globalCompanyId, java.sql.Timestamp.from(Instant.now()));
        assertThat(permissionAdapter.hasPermission(globalAdminId, "marketing:campaign:create", globalCompanyId,
                AccessScope.COMPANY)).isTrue();
    }
}
