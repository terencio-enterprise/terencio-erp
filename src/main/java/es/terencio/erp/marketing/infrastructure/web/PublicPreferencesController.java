package es.terencio.erp.marketing.infrastructure.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.marketing.application.dto.UnsubscribeRequest;
import es.terencio.erp.marketing.application.port.in.ManagePreferencesUseCase;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/public/marketing")
@RequiredArgsConstructor
@Tag(name = "Public Marketing Preferences", description = "Public endpoints for email preferences and unsubscribe flows")
public class PublicPreferencesController {

    private final ManagePreferencesUseCase preferencesUseCase;

    @GetMapping("/preferences")
    @Operation(summary = "Get preferences", description = "Returns marketing preferences for a secure token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPreferences(@RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.success(preferencesUseCase.getPreferences(token)));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update preferences", description = "Updates marketing preferences")
    public ResponseEntity<ApiResponse<Void>> updatePreferences(@RequestBody UnsubscribeRequest request) {
        preferencesUseCase.updatePreferences(request);
        return ResponseEntity.ok(ApiResponse.success("Preferences updated"));
    }

    @PostMapping("/unsubscribe-one-click")
    @Operation(summary = "One-click unsubscribe", description = "Unsubscribes a contact using one-click token")
    public ResponseEntity<ApiResponse<Void>> unsubscribeOneClick(@RequestParam String token) {
        preferencesUseCase.unsubscribeOneClick(token);
        return ResponseEntity.ok(ApiResponse.success("Unsubscribed successfully"));
    }
}
