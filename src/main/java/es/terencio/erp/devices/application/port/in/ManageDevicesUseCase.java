package es.terencio.erp.devices.application.port.in;

import java.util.List;
import java.util.UUID;

import es.terencio.erp.devices.application.dto.DeviceDto;
import es.terencio.erp.devices.application.dto.GenerateCodeRequest;
import es.terencio.erp.devices.application.dto.GeneratedCodeDto;

public interface ManageDevicesUseCase {
    List<DeviceDto> listAll();
    void blockDevice(UUID id);
    void unblockDevice(UUID id);
    GeneratedCodeDto generateRegistrationCode(GenerateCodeRequest request);
}