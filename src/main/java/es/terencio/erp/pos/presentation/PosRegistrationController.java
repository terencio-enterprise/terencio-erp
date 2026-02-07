package es.terencio.erp.pos.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.pos.application.dto.PosRegistrationConfirmRequest;
import es.terencio.erp.pos.application.dto.PosRegistrationPreviewDto;
import es.terencio.erp.pos.application.dto.PosRegistrationPreviewRequest;
import es.terencio.erp.pos.application.dto.PosRegistrationResultDto;
import es.terencio.erp.pos.application.port.in.PosRegistrationUseCase;
import jakarta.validation.Valid;

/**
 * REST controller for POS registration endpoints.
 * This is the presentation layer for the POS module.
 */
@RestController
@RequestMapping("/api/v1/pos/registration")
@CrossOrigin(origins = "*")
public class PosRegistrationController {

    private final PosRegistrationUseCase registrationUseCase;

    public PosRegistrationController(PosRegistrationUseCase registrationUseCase) {
        this.registrationUseCase = registrationUseCase;
    }

    /**
     * Preview registration: validate code and return store/user context.
     * 
     * POST /api/v1/pos/registration/preview
     */
    @PostMapping("/preview")
    public ResponseEntity<PosRegistrationPreviewDto> preview(
            @Valid @RequestBody PosRegistrationPreviewRequest request) {
        return ResponseEntity.ok(registrationUseCase.previewRegistration(request.code()));
    }

    /**
     * Confirm registration: create device and return configuration.
     * 
     * POST /api/v1/pos/registration/confirm
     */
    @PostMapping("/confirm")
    public ResponseEntity<PosRegistrationResultDto> confirm(
            @Valid @RequestBody PosRegistrationConfirmRequest request) {
        return ResponseEntity.ok(
                registrationUseCase.confirmRegistration(request.code(), request.hardwareId()));
    }
}
