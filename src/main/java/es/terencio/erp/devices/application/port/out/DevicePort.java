package es.terencio.erp.devices.application.port.out;

import java.util.List;
import java.util.UUID;

import es.terencio.erp.devices.application.dto.DeviceDto;

public interface DevicePort {
    List<DeviceDto> findAll();
    void updateStatus(UUID id, String status);
}