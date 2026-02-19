package es.terencio.erp.devices;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import es.terencio.erp.AbstractIntegrationTest;
import es.terencio.erp.devices.application.dto.DeviceContextDto;
import es.terencio.erp.devices.application.dto.GenerateCodeRequest;
import es.terencio.erp.devices.application.dto.GeneratedCodeDto;
import es.terencio.erp.devices.application.dto.SetupPreviewDto;
import es.terencio.erp.devices.application.dto.SetupResultDto;
import es.terencio.erp.shared.presentation.ApiResponse;

class DeviceRegistrationIntegrationTest extends AbstractIntegrationTest {

        @BeforeEach
        void setUp() {
                // Ensure some standard employees exist for testing the Context DTO sync
                Long cashier1Id = jdbcClient.sql(
                                "INSERT INTO employees (username, full_name, pin_hash, password_hash, organization_id, is_active) VALUES ('cashier1', 'Test Cashier 1', 'pinhash1', 'passhash1', :orgId, TRUE) RETURNING id")
                                .param("orgId", globalOrgId).query(Long.class).single();
                jdbcClient.sql("INSERT INTO employee_access_grants (employee_id, scope, target_id, role, created_at) VALUES (:empId, 'STORE', :storeId, 'CASHIER', NOW())")
                                .param("empId", cashier1Id).param("storeId", globalStoreId).update();
        }

        @Test
        void testCompleteDeviceRegistrationFlow() {
                GenerateCodeRequest codeRequest = new GenerateCodeRequest(globalStoreId, "Test POS Terminal", 24);

                ResponseEntity<ApiResponse<GeneratedCodeDto>> codeResponse = restTemplate.exchange(
                                "/api/v1/admin/devices/generate-code?storeId=" + globalStoreId, HttpMethod.POST,
                                new HttpEntity<>(codeRequest, globalAdminHeaders),
                                new ParameterizedTypeReference<ApiResponse<GeneratedCodeDto>>() {
                                });

                String registrationCode = java.util.Objects.requireNonNull(codeResponse.getBody()).getData().code();

                ResponseEntity<ApiResponse<SetupPreviewDto>> previewResponse = restTemplate.exchange(
                                "/api/v1/public/devices/preview/" + registrationCode, HttpMethod.GET, null,
                                new ParameterizedTypeReference<ApiResponse<SetupPreviewDto>>() {
                                });

                SetupPreviewDto preview = java.util.Objects.requireNonNull(previewResponse.getBody()).getData();
                assertThat(preview.storeName()).isEqualTo("Global Test Store");

                String hardwareId = "TEST-HARDWARE-" + UUID.randomUUID();
                ResponseEntity<ApiResponse<SetupResultDto>> setupResponse = restTemplate.exchange(
                                "/api/v1/public/devices/confirm/" + registrationCode + "?hardwareId=" + hardwareId,
                                HttpMethod.POST,
                                null, new ParameterizedTypeReference<ApiResponse<SetupResultDto>>() {
                                });

                SetupResultDto setup = java.util.Objects.requireNonNull(setupResponse.getBody()).getData();
                String apiKey = setup.apiKey();

                HttpHeaders headers = new HttpHeaders();
                headers.set("X-API-Key", apiKey);
                ResponseEntity<ApiResponse<DeviceContextDto>> contextResponse = restTemplate.exchange(
                                "/api/v1/pos/sync/context", HttpMethod.GET, new HttpEntity<>(headers),
                                new ParameterizedTypeReference<ApiResponse<DeviceContextDto>>() {
                                });

                DeviceContextDto context = java.util.Objects.requireNonNull(contextResponse.getBody()).getData();

                assertThat(context.store().code()).isEqualTo("GLOBAL-STORE");
                assertThat(context.users()).extracting("username").contains("admin", "cashier1");
        }
}