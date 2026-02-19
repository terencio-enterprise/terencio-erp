package es.terencio.erp.devices;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;

import es.terencio.erp.AbstractIntegrationTest;
import es.terencio.erp.auth.application.dto.AuthDtos.LoginRequest;
import es.terencio.erp.auth.application.dto.AuthDtos.LoginResponse;
import es.terencio.erp.devices.application.dto.DeviceContextDto;
import es.terencio.erp.devices.application.dto.DeviceDto;
import es.terencio.erp.devices.application.dto.GenerateCodeRequest;
import es.terencio.erp.devices.application.dto.GeneratedCodeDto;
import es.terencio.erp.devices.application.dto.SetupPreviewDto;
import es.terencio.erp.devices.application.dto.SetupResultDto;
import es.terencio.erp.shared.presentation.ApiResponse;

class DeviceRegistrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcClient jdbcClient;
    @Autowired private PasswordEncoder passwordEncoder;

    private UUID testStoreId;
    private String adminToken;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        UUID testCompanyId = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO companies (id, name, tax_id, is_active) VALUES (:id, 'Test Company', 'B12345678', TRUE)").param("id", testCompanyId).update();
        testStoreId = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO stores (id, company_id, code, name, address, is_active) VALUES (:id, :companyId, 'MAD-TEST', 'Test Store Madrid', 'Test Address', TRUE)").param("id", testStoreId).param("companyId", testCompanyId).update();
        jdbcClient.sql("INSERT INTO store_settings (store_id, allow_negative_stock, print_ticket_automatically) VALUES (:storeId, FALSE, TRUE)").param("storeId", testStoreId).update();

        String adminPassword = passwordEncoder.encode("admin123");
        jdbcClient.sql("INSERT INTO employees (username, full_name, role, pin_hash, password_hash, organization_id, store_id, permissions_json, is_active) VALUES ('admindev', 'Admin Device', 'ADMIN', 'pinadmin', :password, (SELECT organization_id FROM companies WHERE id = :companyId), :storeId, '[]', TRUE) RETURNING id")
                .param("password", adminPassword).param("companyId", testCompanyId).param("storeId", testStoreId).query(Long.class).single();

        jdbcClient.sql("INSERT INTO employees (username, full_name, role, pin_hash, password_hash, organization_id, store_id, permissions_json, is_active) VALUES ('cashier1', 'Test Cashier 1', 'CASHIER', 'pinhash1', 'passhash1', (SELECT organization_id FROM companies WHERE id = :companyId), :storeId, '[]', TRUE) RETURNING id")
                .param("companyId", testCompanyId).param("storeId", testStoreId).query(Long.class).single();

        jdbcClient.sql("INSERT INTO employees (username, full_name, role, pin_hash, password_hash, organization_id, store_id, permissions_json, is_active) VALUES ('manager1', 'Test Manager 1', 'MANAGER', 'pinhash2', 'passhash2', (SELECT organization_id FROM companies WHERE id = :companyId), :storeId, '[]', TRUE) RETURNING id")
                .param("companyId", testCompanyId).param("storeId", testStoreId).query(Long.class).single();

        LoginRequest loginRequest = new LoginRequest("admindev", "admin123");
        ResponseEntity<ApiResponse<LoginResponse>> loginResponse = restTemplate.exchange("/api/v1/auth/login", HttpMethod.POST, new HttpEntity<>(loginRequest), new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {});
        adminToken = java.util.Objects.requireNonNull(loginResponse.getBody()).getData().token();
    }

    @Test
    void testCompleteDeviceRegistrationFlow() {
        GenerateCodeRequest codeRequest = new GenerateCodeRequest(testStoreId, "Test POS Terminal", 24);
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        ResponseEntity<ApiResponse<GeneratedCodeDto>> codeResponse = restTemplate.exchange("/api/v1/admin/devices/generate-code", HttpMethod.POST, new HttpEntity<>(codeRequest, adminHeaders), new ParameterizedTypeReference<ApiResponse<GeneratedCodeDto>>() {});
        
        String registrationCode = java.util.Objects.requireNonNull(codeResponse.getBody()).getData().code();

        ResponseEntity<ApiResponse<SetupPreviewDto>> previewResponse = restTemplate.exchange("/api/v1/public/devices/preview/" + registrationCode, HttpMethod.GET, null, new ParameterizedTypeReference<ApiResponse<SetupPreviewDto>>() {});
        SetupPreviewDto preview = java.util.Objects.requireNonNull(previewResponse.getBody()).getData();
        assertThat(preview.storeName()).isEqualTo("Test Store Madrid");

        String hardwareId = "TEST-HARDWARE-" + UUID.randomUUID();
        ResponseEntity<ApiResponse<SetupResultDto>> setupResponse = restTemplate.exchange("/api/v1/public/devices/confirm/" + registrationCode + "?hardwareId=" + hardwareId, HttpMethod.POST, null, new ParameterizedTypeReference<ApiResponse<SetupResultDto>>() {});
        SetupResultDto setup = java.util.Objects.requireNonNull(setupResponse.getBody()).getData();
        String apiKey = setup.apiKey();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        ResponseEntity<ApiResponse<DeviceContextDto>> contextResponse = restTemplate.exchange("/api/v1/pos/sync/context", HttpMethod.GET, new HttpEntity<>(headers), new ParameterizedTypeReference<ApiResponse<DeviceContextDto>>() {});
        DeviceContextDto context = java.util.Objects.requireNonNull(contextResponse.getBody()).getData();

        assertThat(context.store().code()).isEqualTo("MAD-TEST");
        assertThat(context.users()).extracting("username").contains("cashier1", "manager1");
    }
}
