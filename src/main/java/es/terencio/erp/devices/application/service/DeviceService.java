package es.terencio.erp.devices.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.devices.application.dto.DeviceDto;
import es.terencio.erp.devices.application.port.in.ManageDevicesUseCase;
import es.terencio.erp.devices.application.port.out.DevicePort;

@Service
public class DeviceService implements ManageDevicesUseCase {

    private final DevicePort devicePort;

    public DeviceService(DevicePort devicePort) {
        this.devicePort = devicePort;
    }

    @Override
    public List<DeviceDto> listAll() {
        return devicePort.findAll();
    }

    @Override
    @Transactional
    public void blockDevice(UUID id) {
        devicePort.updateStatus(id, "BLOCKED");
    }

    @Override
    @Transactional
    public void unblockDevice(UUID id) {
        devicePort.updateStatus(id, "ACTIVE");
    }
}