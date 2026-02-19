package es.terencio.erp.auth.infrastructure.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.terencio.erp.AbstractIntegrationTest;
import es.terencio.erp.auth.application.dto.AuthDtos.LoginRequest;
import es.terencio.erp.auth.application.dto.AuthDtos.LoginResponse;
import es.terencio.erp.shared.presentation.ApiResponse;

class AuthenticationIntegrationTest extends AbstractIntegrationTest {

        @Test
        void testSuccessfulLogin() {
                // Relies on the globalAdmin created in AbstractIntegrationTest
                ResponseEntity<ApiResponse<LoginResponse>> response = restTemplate.exchange("/api/v1/auth/login",
                                HttpMethod.POST, new HttpEntity<>(new LoginRequest("admin", "admin123")),
                                new ParameterizedTypeReference<>() {
                                });
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().getData().token()).isNotBlank();
        }
}