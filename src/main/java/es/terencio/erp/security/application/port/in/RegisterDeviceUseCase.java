package es.terencio.erp.security.application.port.in;

import es.terencio.erp.security.application.usecase.RegisterDeviceCommand;
import es.terencio.erp.security.application.usecase.RegisterDeviceResult;

/**
 * Input port for device registration.
 */
public interface RegisterDeviceUseCase {
    RegisterDeviceResult execute(RegisterDeviceCommand command);
}
