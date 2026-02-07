package es.terencio.erp.infrastructure.adapter.out.persistence;

import es.terencio.erp.domain.model.Store;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class RegistrationRepository {

    private final JdbcClient jdbcClient;

    public RegistrationRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

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

    // Helper Record for the Join Query
    public record RegistrationInfo(
        String code, 
        String preassignedName, 
        java.time.Instant expiresAt, 
        Boolean isUsed,
        UUID storeId,
        String storeName,
        String storeCode
    ) {}
}