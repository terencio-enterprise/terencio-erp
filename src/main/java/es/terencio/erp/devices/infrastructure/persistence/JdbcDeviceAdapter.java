package es.terencio.erp.devices.infrastructure.persistence;

import java.util.List;
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
    public void updateStatus(UUID id, String status) {
        jdbcClient.sql("UPDATE devices SET status = :status WHERE id = :id")
                .param("id", id)
                .param("status", status)
                .update();
    }
}