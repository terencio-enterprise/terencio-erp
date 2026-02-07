package es.terencio.erp.pos.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.pos.application.port.out.RegistrationPort;

/**
 * JDBC adapter implementing the RegistrationPort.
 * This is the infrastructure implementation of the output port.
 */
@Repository
public class JdbcRegistrationAdapter implements RegistrationPort {

    private final JdbcClient jdbcClient;

    public JdbcRegistrationAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<RegistrationInfo> findByCode(String code) {
        String sql = """
                    SELECT rc.code, rc.preassigned_name, rc.expires_at, rc.is_used,
                           s.id as store_id, s.name as store_name, s.code as store_code
                    FROM registration_codes rc
                    JOIN stores s ON rc.store_id = s.id
                    WHERE rc.code = :code
                """;

        return jdbcClient.sql(sql)
                .param("code", code)
                .query(RegistrationInfo.class)
                .optional();
    }

    @Override
    @Transactional
    public UUID registerDevice(String code, String hardwareId, UUID storeId, String serialCode) {
        // 1. Create Device
        UUID deviceId = UUID.randomUUID();
        String sqlDevice = """
                    INSERT INTO devices (id, store_id, serial_code, hardware_id, status, last_sync_at)
                    VALUES (:id, :storeId, :serialCode, :hardwareId, 'ACTIVE', NOW())
                """;

        jdbcClient.sql(sqlDevice)
                .param("id", deviceId)
                .param("storeId", storeId)
                .param("serialCode", serialCode)
                .param("hardwareId", hardwareId)
                .update();

        // 2. Mark Code as Used
        String sqlCode = """
                    UPDATE registration_codes
                    SET is_used = TRUE, used_at = NOW(), used_by_device_id = :deviceId
                    WHERE code = :code
                """;

        jdbcClient.sql(sqlCode)
                .param("deviceId", deviceId)
                .param("code", code)
                .update();

        return deviceId;
    }
}
