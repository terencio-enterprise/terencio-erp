package es.terencio.erp.application.port.in;

import es.terencio.erp.application.dto.PosRegistrationPreviewDto;
import es.terencio.erp.application.dto.PosRegistrationResultDto;

public interface PosRegistrationUseCase {
    
    /**
     * Step 1: Validate the 6-digit code and return context (Store Name, POS Name)
     */
    PosRegistrationPreviewDto previewRegistration(String code);

    /**
     * Step 2: Finalize registration, create Device record, and return Config + Users
     */
    PosRegistrationResultDto confirmRegistration(String code, String hardwareId);
}