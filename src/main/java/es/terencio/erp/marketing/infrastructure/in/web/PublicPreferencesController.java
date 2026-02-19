package es.terencio.erp.marketing.infrastructure.in.web;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import es.terencio.erp.marketing.application.dto.MarketingDtos.UnsubscribeRequest;
import es.terencio.erp.marketing.application.port.in.ManagePreferencesUseCase;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/public/marketing")
@Tag(name = "Public Marketing Preferences", description = "Public endpoints for email preferences")
public class PublicPreferencesController {

    private final ManagePreferencesUseCase preferencesUseCase;

    public PublicPreferencesController(ManagePreferencesUseCase preferencesUseCase) {
        this.preferencesUseCase = preferencesUseCase;
    }

    @GetMapping("/preferences")
    @Operation(summary = "Get preferences")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPreferences(@RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.success(preferencesUseCase.getPreferences(token)));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update preferences")
    public ResponseEntity<ApiResponse<Void>> updatePreferences(@RequestBody UnsubscribeRequest request) {
        preferencesUseCase.updatePreferences(request);
        return ResponseEntity.ok(ApiResponse.success("Preferences updated"));
    }

    @PostMapping("/unsubscribe-one-click")
    @Operation(summary = "One-click unsubscribe")
    public ResponseEntity<ApiResponse<Void>> unsubscribeOneClick(@RequestParam String token) {
        preferencesUseCase.unsubscribeOneClick(token);
        return ResponseEntity.ok(ApiResponse.success("Unsubscribed successfully"));
    }
}
