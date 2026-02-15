package es.terencio.erp.devices.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.terencio.erp.devices.application.dto.DeviceDto;
import es.terencio.erp.devices.application.port.out.DevicePort;

@Repository
public class JdbcDeviceAdapter implements DevicePort {

    private final JdbcClient jdbcClient;

    public JdbcDeviceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<DeviceDto> findAll() {
        String sql = """
                    SELECT d.id, d.store_id, s.name as store_name, d.serial_code,
                           d.hardware_id, d.status, d.version_app, d.last_sync_at, d.created_at
                    FROM devices d
                    JOIN stores s ON d.store_id = s.id
                    ORDER BY d.created_at DESC
                """;
        return jdbcClient.sql(sql).query(DeviceDto.class).list();
    }

    @Override
    public Optional<DeviceDto> findById(UUID id) {
        String sql = """
                    SELECT d.id, d.store_id, s.name as store_name, d.serial_code,
                           d.hardware_id, d.status, d.version_app, d.last_sync_at, d.created_at
                    FROM devices d
                    JOIN stores s ON d.store_id = s.id
                    WHERE d.id = :id
                """;
        return jdbcClient.sql(sql).param("id", id).query(DeviceDto.class).optional();
    }

    @Override
    public void updateStatus(UUID id, String status) {
        jdbcClient.sql("UPDATE devices SET status = :status WHERE id = :id")
                .param("id", id).param("status", status).update();
    }

    @Override
    public void saveCode(String code, UUID storeId, String posName, Instant expiresAt) {
        String sql = """
                    INSERT INTO registration_codes (code, store_id, preassigned_name, expires_at, is_used, created_at)
                    VALUES (:code, :storeId, :posName, :expiresAt, FALSE, NOW())
                """;
        jdbcClient.sql(sql)
                .param("code", code).param("storeId", storeId)
                .param("posName", posName).param("expiresAt", Timestamp.from(expiresAt))
                .update();
    }

    @Override
    public Optional<CodeInfo> findByCode(String code) {
        String sql = """
                    SELECT rc.code, rc.preassigned_name, rc.expires_at, rc.is_used,
                           s.id as store_id, s.name as store_name, s.code as store_code
                    FROM registration_codes rc
                    JOIN stores s ON rc.store_id = s.id
                    WHERE rc.code = :code
                """;
        return jdbcClient.sql(sql).param("code", code)
                .query((rs, rowNum) -> new CodeInfo(
                        rs.getString("code"),
                        rs.getObject("store_id", UUID.class),
                        rs.getString("store_name"),
                        rs.getString("store_code"),
                        rs.getString("preassigned_name"),
                        rs.getTimestamp("expires_at").toInstant(),
                        rs.getBoolean("is_used")))
                .optional();
    }

    @Override
    public UUID registerDevice(String code, String hardwareId, UUID storeId, String serialCode,
            String deviceSecret) {
        UUID deviceId = UUID.randomUUID();

        jdbcClient
                .sql("""
                        INSERT INTO devices (id, store_id, serial_code, hardware_id, status, device_secret, api_key_version, last_sync_at)
                        VALUES (:id, :storeId, :serialCode, :hardwareId, 'ACTIVE', :secret, 1, NOW())
                        """)
                .param("id", deviceId)
                .param("storeId", storeId)
                .param("serialCode", serialCode)
                .param("hardwareId", hardwareId)
                .param("secret", deviceSecret)
                .update();

        jdbcClient.sql(
                "UPDATE registration_codes SET is_used = TRUE, used_at = NOW(), used_by_device_id = :deviceId WHERE code = :code")
                .param("deviceId", deviceId).param("code", code)
                .update();

        return deviceId;
    }
}