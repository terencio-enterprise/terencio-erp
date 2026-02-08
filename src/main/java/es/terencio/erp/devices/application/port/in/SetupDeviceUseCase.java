package es.terencio.erp.devices.application.port.in;

import es.terencio.erp.devices.application.dto.SetupPreviewDto;
import es.terencio.erp.devices.application.dto.SetupResultDto;

public interface SetupDeviceUseCase {
    SetupPreviewDto previewSetup(String code);
    SetupResultDto confirmSetup(String code, String hardwareId);
}