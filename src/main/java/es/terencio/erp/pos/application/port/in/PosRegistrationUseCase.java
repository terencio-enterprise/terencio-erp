package es.terencio.erp.pos.application.port.in;

import es.terencio.erp.pos.application.dto.PosRegistrationPreviewDto;
import es.terencio.erp.pos.application.dto.PosRegistrationResultDto;

/**
 * Input port (use case interface) for POS registration.
 * Defines the business operations available for POS registration.
 */
public interface PosRegistrationUseCase {

    /**
     * Step 1: Validate the registration code and return context (Store Name, POS
     * Name, Users).
     * 
     * @param code the 6-digit registration code
     * @return preview information
     */
    PosRegistrationPreviewDto previewRegistration(String code);

    /**
     * Step 2: Finalize registration, create Device record, and return Config +
     * License.
     * 
     * @param code       the registration code
     * @param hardwareId the device hardware ID
     * @return registration result
     */
    PosRegistrationResultDto confirmRegistration(String code, String hardwareId);
}