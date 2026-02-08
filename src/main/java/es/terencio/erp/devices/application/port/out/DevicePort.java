package es.terencio.erp.devices.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import es.terencio.erp.devices.application.dto.DeviceDto;

public interface DevicePort {
    List<DeviceDto> findAll();
    void updateStatus(UUID id, String status);
    
    // Registration Methods
    void saveCode(String code, UUID storeId, String posName, Instant expiresAt);
    Optional<CodeInfo> findByCode(String code);
    UUID registerDevice(String code, String hardwareId, UUID storeId, String serialCode);
    
    record CodeInfo(String code, UUID storeId, String storeName, String storeCode, String preassignedName, Instant expiresAt, boolean isUsed) {}
}