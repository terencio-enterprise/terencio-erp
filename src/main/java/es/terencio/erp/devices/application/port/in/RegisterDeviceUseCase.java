package es.terencio.erp.devices.application.port.in;

import es.terencio.erp.devices.application.usecase.RegisterDeviceCommand;
import es.terencio.erp.devices.application.usecase.RegisterDeviceResult;

/**
 * Input port for device registration.
 */
public interface RegisterDeviceUseCase {
    RegisterDeviceResult execute(RegisterDeviceCommand command);
}
