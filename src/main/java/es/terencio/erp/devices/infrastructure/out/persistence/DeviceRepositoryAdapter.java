package es.terencio.erp.devices.infrastructure.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.terencio.erp.devices.application.dto.DeviceDto;
import es.terencio.erp.devices.application.port.out.DevicePort;

@Repository
public class DeviceRepositoryAdapter implements DevicePort {
    
    private final JdbcClient jdbcClient;

    public DeviceRepositoryAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<DeviceDto> findAll() {
        return jdbcClient.sql("SELECT d.id, d.store_id, s.name as store_name, d.serial_code, d.hardware_id, d.status, d.version_app, d.last_sync_at, d.created_at FROM devices d JOIN stores s ON d.store_id = s.id")
            .query((rs, rowNum) -> new DeviceDto(
                rs.getObject("id", UUID.class), rs.getObject("store_id", UUID.class),
                rs.getString("store_name"), rs.getString("serial_code"),
                rs.getString("hardware_id"), rs.getString("status"),
                rs.getString("version_app"), 
                rs.getTimestamp("last_sync_at") != null ? rs.getTimestamp("last_sync_at").toInstant() : null,
                rs.getTimestamp("created_at").toInstant()
            )).list();
    }

    @Override
    public void updateStatus(UUID id, String status) {
        jdbcClient.sql("UPDATE devices SET status = :status, updated_at = NOW() WHERE id = :id")
            .param("status", status).param("id", id).update();
    }

    @Override
    public void saveCode(String code, UUID storeId, String posName, Instant expiresAt) {
        jdbcClient.sql("INSERT INTO registration_codes (code, store_id, preassigned_name, expires_at) VALUES (:code, :storeId, :posName, :expiresAt)")
            .param("code", code).param("storeId", storeId).param("posName", posName).param("expiresAt", java.sql.Timestamp.from(expiresAt)).update();
    }

    @Override
    public Optional<CodeInfo> findByCode(String code) {
        return jdbcClient.sql("SELECT c.code, c.store_id, s.name as store_name, s.code as store_code, c.preassigned_name, c.expires_at, c.is_used FROM registration_codes c JOIN stores s ON c.store_id = s.id WHERE c.code = :code")
            .param("code", code)
            .query((rs, rowNum) -> new CodeInfo(
                rs.getString("code"), rs.getObject("store_id", UUID.class), rs.getString("store_name"),
                rs.getString("store_code"), rs.getString("preassigned_name"),
                rs.getTimestamp("expires_at").toInstant(), rs.getBoolean("is_used")
            )).optional();
    }

    @Override
    public UUID registerDevice(String code, String hardwareId, UUID storeId, String serialCode, String deviceSecret) {
        jdbcClient.sql("UPDATE registration_codes SET is_used = TRUE, used_at = NOW() WHERE code = :code").param("code", code).update();
        UUID deviceId = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO devices (id, store_id, serial_code, hardware_id, status, device_secret) VALUES (:id, :storeId, :serialCode, :hardwareId, 'ACTIVE', :secret)")
            .param("id", deviceId).param("storeId", storeId).param("serialCode", serialCode)
            .param("hardwareId", hardwareId).param("secret", deviceSecret).update();
        return deviceId;
    }

    @Override
    public Optional<DeviceDto> findById(UUID deviceId) {
        return jdbcClient.sql("SELECT d.id, d.store_id, s.name as store_name, d.serial_code, d.hardware_id, d.status, d.version_app, d.last_sync_at, d.created_at FROM devices d JOIN stores s ON d.store_id = s.id WHERE d.id = :id")
            .param("id", deviceId)
            .query((rs, rowNum) -> new DeviceDto(
                rs.getObject("id", UUID.class), rs.getObject("store_id", UUID.class),
                rs.getString("store_name"), rs.getString("serial_code"),
                rs.getString("hardware_id"), rs.getString("status"),
                rs.getString("version_app"), 
                rs.getTimestamp("last_sync_at") != null ? rs.getTimestamp("last_sync_at").toInstant() : null,
                rs.getTimestamp("created_at").toInstant()
            )).optional();
    }
}
