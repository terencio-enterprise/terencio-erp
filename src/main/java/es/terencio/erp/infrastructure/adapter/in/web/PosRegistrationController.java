package es.terencio.erp.infrastructure.adapter.in.web;

import es.terencio.erp.application.dto.PosRegistrationPreviewDto;
import es.terencio.erp.application.dto.PosRegistrationResultDto;
import es.terencio.erp.application.port.in.PosRegistrationUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/pos")
@CrossOrigin(origins = "*")
public class PosRegistrationController {

    private final PosRegistrationUseCase registrationUseCase;

    public PosRegistrationController(PosRegistrationUseCase registrationUseCase) {
        this.registrationUseCase = registrationUseCase;
    }

    @PostMapping("/preview")
    public ResponseEntity<PosRegistrationPreviewDto> preview(@RequestBody Map<String, String> payload) {
        String code = payload.get("registrationCode");
        return ResponseEntity.ok(registrationUseCase.previewRegistration(code));
    }

    @PostMapping("/register")
    public ResponseEntity<PosRegistrationResultDto> register(@RequestBody Map<String, String> payload) {
        String code = payload.get("registrationCode");
        String hardwareId = payload.get("hardwareId");
        
        return ResponseEntity.ok(registrationUseCase.confirmRegistration(code, hardwareId));
    }
}