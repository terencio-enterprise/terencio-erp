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
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/public/marketing")
@RequiredArgsConstructor
public class PublicPreferencesController {

    private final ManagePreferencesUseCase preferencesUseCase;

    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPreferences(@RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.success(preferencesUseCase.getPreferences(token)));
    }

    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<Void>> updatePreferences(@RequestBody UnsubscribeRequest request) {
        preferencesUseCase.updatePreferences(request);
        return ResponseEntity.ok(ApiResponse.success("Preferences updated"));
    }

    @PostMapping("/unsubscribe-one-click")
    public ResponseEntity<ApiResponse<Void>> unsubscribeOneClick(@RequestParam String token) {
        preferencesUseCase.unsubscribeOneClick(token);
        return ResponseEntity.ok(ApiResponse.success("Unsubscribed successfully"));
    }
}
