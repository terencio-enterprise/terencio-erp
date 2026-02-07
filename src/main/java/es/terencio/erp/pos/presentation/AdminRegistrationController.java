package es.terencio.erp.pos.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.pos.application.dto.GenerateCodeRequest;
import es.terencio.erp.pos.application.dto.GeneratedCodeDto;
import es.terencio.erp.pos.application.port.in.ManageRegistrationCodeUseCase;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/registration")
@CrossOrigin(origins = "*")
public class AdminRegistrationController {

    private final ManageRegistrationCodeUseCase manageUseCase;

    public AdminRegistrationController(ManageRegistrationCodeUseCase manageUseCase) {
        this.manageUseCase = manageUseCase;
    }

    /**
     * POST /api/v1/admin/registration/generate
     * Admin creates a code for a new POS.
     */
    @PostMapping("/generate")
    public ResponseEntity<GeneratedCodeDto> generate(@Valid @RequestBody GenerateCodeRequest request) {
        return ResponseEntity.ok(manageUseCase.generateCode(request));
    }
}