package es.terencio.erp.devices;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;

import es.terencio.erp.AbstractIntegrationTest;
import es.terencio.erp.auth.application.dto.LoginRequest;
import es.terencio.erp.auth.application.dto.LoginResponse;
import es.terencio.erp.devices.application.dto.DeviceContextDto;
import es.terencio.erp.devices.application.dto.GenerateCodeRequest;
import es.terencio.erp.devices.application.dto.GeneratedCodeDto;
import es.terencio.erp.devices.application.dto.SetupPreviewDto;
import es.terencio.erp.devices.application.dto.SetupResultDto;

/**
 * Integration test for the complete device registration flow.
 * Tests the "happy path" scenario:
 * 1. Admin generates a registration code
 * 2. Public client calls previewSetup
 * 3. Public client calls confirmSetup -> receives API Key
 * 4. Client uses API Key to call GET /api/v1/pos/sync/context
 * 5. Assert that the context returns the correct Store and Users
 */
class DeviceRegistrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID testCompanyId;
    private UUID testStoreId;
    private Long testUserId1;
    private Long testUserId2;
    private Long adminUserId;
    private String adminToken;

    @BeforeEach
    void setUp() {
        // Create test company
        testCompanyId = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO companies (id, name, tax_id, is_active)
                VALUES (:id, 'Test Company', 'B12345678', TRUE)
                """)
                .param("id", testCompanyId)
                .update();

        // Create test store
        testStoreId = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO stores (id, company_id, code, name, address, is_active)
                VALUES (:id, :companyId, 'MAD-TEST', 'Test Store Madrid', 'Test Address', TRUE)
                """)
                .param("id", testStoreId)
                .param("companyId", testCompanyId)
                .update();

        // Create store settings
        jdbcClient.sql("""
                INSERT INTO store_settings (store_id, allow_negative_stock, print_ticket_automatically)
                VALUES (:storeId, FALSE, TRUE)
                """)
                .param("storeId", testStoreId)
                .update();

        // Create admin user for authentication
        String adminPassword = passwordEncoder.encode("admin123");
        adminUserId = jdbcClient.sql("""
                INSERT INTO users (username, full_name, role, pin_hash, password_hash,
                    company_id, store_id, permissions_json, is_active)
                VALUES ('admindev', 'Admin Device', 'ADMIN', 'pinadmin', :password,
                    :companyId, :storeId, '[]', TRUE)
                RETURNING id
                """)
                .param("password", adminPassword)
                .param("companyId", testCompanyId)
                .param("storeId", testStoreId)
                .query(Long.class)
                .single();

        // Create test users for the store
        testUserId1 = jdbcClient
                .sql("""
                        INSERT INTO users (username, full_name, role, pin_hash, password_hash, company_id, store_id, permissions_json, is_active)
                        VALUES ('cashier1', 'Test Cashier 1', 'CASHIER', 'pinhash1', 'passhash1', :companyId, :storeId, '[]', TRUE)
                        RETURNING id
                        """)
                .param("companyId", testCompanyId)
                .param("storeId", testStoreId)
                .query(Long.class)
                .single();

        testUserId2 = jdbcClient
                .sql("""
                        INSERT INTO users (username, full_name, role, pin_hash, password_hash, company_id, store_id, permissions_json, is_active)
                        VALUES ('manager1', 'Test Manager 1', 'MANAGER', 'pinhash2', 'passhash2', :companyId, :storeId, '[]', TRUE)
                        RETURNING id
                        """)
                .param("companyId", testCompanyId)
                .param("storeId", testStoreId)
                .query(Long.class)
                .single();

        // Authenticate as admin and get JWT token
        LoginRequest loginRequest = new LoginRequest("admindev", "admin123");
        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login",
                loginRequest,
                LoginResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        adminToken = loginResponse.getBody().token();
        assertThat(adminToken).isNotBlank();
    }

    @AfterEach
    void cleanup() {
        // Use TRUNCATE CASCADE to automatically handle all foreign key constraints
        jdbcClient.sql(
                "TRUNCATE TABLE devices, registration_codes, users, store_settings, warehouses, stores, companies CASCADE")
                .update();
    }

    @Test
    void testCompleteDeviceRegistrationFlow() {
        // ============================================================
        // Step 1: Admin generates a registration code
        // ============================================================
        GenerateCodeRequest codeRequest = new GenerateCodeRequest(
                testStoreId,
                "Test POS Terminal",
                24 // validity hours
        );

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        HttpEntity<GenerateCodeRequest> codeRequestEntity = new HttpEntity<>(codeRequest, adminHeaders);

        ResponseEntity<GeneratedCodeDto> codeResponse = restTemplate.exchange(
                "/api/v1/admin/devices/generate-code",
                HttpMethod.POST,
                codeRequestEntity,
                GeneratedCodeDto.class);

        assertThat(codeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(codeResponse.getBody()).isNotNull();
        String registrationCode = codeResponse.getBody().code();
        assertThat(registrationCode).isNotBlank();

        // ============================================================
        // Step 2: Public client calls previewSetup
        // ============================================================
        ResponseEntity<SetupPreviewDto> previewResponse = restTemplate.getForEntity(
                "/api/v1/public/devices/preview/" + registrationCode,
                SetupPreviewDto.class);

        assertThat(previewResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(previewResponse.getBody()).isNotNull();
        SetupPreviewDto preview = previewResponse.getBody();
        assertThat(preview.storeName()).isEqualTo("Test Store Madrid");
        assertThat(preview.users()).hasSize(2);
        assertThat(preview.users()).extracting("username").contains("cashier1", "manager1");

        // ============================================================
        // Step 3: Public client calls confirmSetup -> receives API Key
        // ============================================================
        String hardwareId = "TEST-HARDWARE-" + UUID.randomUUID();
        String confirmUrl = "/api/v1/public/devices/confirm/" + registrationCode + "?hardwareId=" + hardwareId;

        ResponseEntity<SetupResultDto> setupResponse = restTemplate.postForEntity(
                confirmUrl,
                null,
                SetupResultDto.class);

        assertThat(setupResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(setupResponse.getBody()).isNotNull();
        SetupResultDto setup = setupResponse.getBody();
        assertThat(setup.apiKey()).isNotBlank();
        UUID deviceId = setup.deviceId();
        String apiKey = setup.apiKey();

        // ============================================================
        // Step 4: Client uses API Key to call GET /api/v1/pos/sync/context
        // ============================================================
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<DeviceContextDto> contextResponse = restTemplate.exchange(
                "/api/v1/pos/sync/context",
                HttpMethod.GET,
                entity,
                DeviceContextDto.class);

        assertThat(contextResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(contextResponse.getBody()).isNotNull();

        // ============================================================
        // Step 5: Assert that the context returns the correct Store and Users
        // ============================================================
        DeviceContextDto context = contextResponse.getBody();

        // Verify Store information
        assertThat(context.store()).isNotNull();
        assertThat(context.store().id()).isEqualTo(testStoreId);
        assertThat(context.store().name()).isEqualTo("Test Store Madrid");
        assertThat(context.store().code()).isEqualTo("MAD-TEST");

        // Verify Store Settings
        assertThat(context.settings()).isNotNull();
        assertThat(context.settings().storeId()).isEqualTo(testStoreId);
        assertThat(context.settings().allowNegativeStock()).isFalse();
        assertThat(context.settings().printTicketAutomatically()).isTrue();

        // Verify Users
        assertThat(context.users()).isNotNull();
        assertThat(context.users()).hasSize(2);
        assertThat(context.users()).extracting("username").contains("cashier1", "manager1");
        assertThat(context.users()).extracting("role").contains("CASHIER", "MANAGER");

        // Verify pinHash is present (for offline verification)
        assertThat(context.users()).allMatch(user -> user.pinHash() != null && !user.pinHash().isBlank());
        assertThat(context.users().get(0).pinHash()).isEqualTo("pinhash1");
    }

    @Test
    @Disabled("TestSecurityConfig permits all requests - security checks are disabled in test environment")
    void testDeviceNotActiveCannotSync() {
        // Create a blocked device
        UUID blockedDeviceId = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO devices (id, store_id, serial_code, hardware_id, status, device_secret, api_key_version)
                VALUES (:id, :storeId, 'BLOCKED-01', 'hw-blocked', 'BLOCKED', 'secret', 1)
                """)
                .param("id", blockedDeviceId)
                .param("storeId", testStoreId)
                .update();

        // Generate API key for blocked device (manually for testing)
        // In real scenario, this would use DeviceApiKeyGenerator
        // For now, we'll just try to call with a fake key and verify it fails

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", "fake-api-key");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<DeviceContextDto> contextResponse = restTemplate.exchange(
                "/api/v1/pos/sync/context",
                HttpMethod.GET,
                entity,
                DeviceContextDto.class);

        // Should return 401 Unauthorized because the API key is invalid
        assertThat(contextResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testInvalidCodePreview() {
        // Try to preview with invalid code
        ResponseEntity<SetupPreviewDto> response = restTemplate.getForEntity(
                "/api/v1/public/devices/preview/INVALID999",
                SetupPreviewDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testCodeCannotBeReused() {
        // Generate code
        GenerateCodeRequest codeRequest = new GenerateCodeRequest(
                testStoreId,
                "Test POS",
                24);

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        HttpEntity<GenerateCodeRequest> codeRequestEntity = new HttpEntity<>(codeRequest, adminHeaders);

        ResponseEntity<GeneratedCodeDto> codeResponse = restTemplate.exchange(
                "/api/v1/admin/devices/generate-code",
                HttpMethod.POST,
                codeRequestEntity,
                GeneratedCodeDto.class);

        assertThat(codeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String code = codeResponse.getBody().code();

        // Use the code once
        String hardwareId1 = "HW-001";
        String confirmUrl1 = "/api/v1/public/devices/confirm/" + code + "?hardwareId=" + hardwareId1;
        ResponseEntity<SetupResultDto> setup1 = restTemplate.postForEntity(confirmUrl1, null, SetupResultDto.class);
        assertThat(setup1.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Try to use the same code again
        String hardwareId2 = "HW-002";
        String confirmUrl2 = "/api/v1/public/devices/confirm/" + code + "?hardwareId=" + hardwareId2;
        ResponseEntity<SetupResultDto> setup2 = restTemplate.postForEntity(confirmUrl2, null, SetupResultDto.class);

        // Should fail because code is already used
        assertThat(setup2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testDeviceBlockAndUnblock() {
        // Create and register a device first
        GenerateCodeRequest codeRequest = new GenerateCodeRequest(testStoreId, "Test POS", 24);
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        HttpEntity<GenerateCodeRequest> codeRequestEntity = new HttpEntity<>(codeRequest, adminHeaders);

        ResponseEntity<GeneratedCodeDto> codeResponse = restTemplate.exchange(
                "/api/v1/admin/devices/generate-code",
                HttpMethod.POST,
                codeRequestEntity,
                GeneratedCodeDto.class);

        String code = codeResponse.getBody().code();
        String hardwareId = "HW-BLOCK-TEST";
        String confirmUrl = "/api/v1/public/devices/confirm/" + code + "?hardwareId=" + hardwareId;

        ResponseEntity<SetupResultDto> setupResponse = restTemplate.postForEntity(
                confirmUrl, null, SetupResultDto.class);
        assertThat(setupResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        UUID deviceId = setupResponse.getBody().deviceId();
        String apiKey = setupResponse.getBody().apiKey();

        // Verify device can sync initially
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        ResponseEntity<DeviceContextDto> contextResponse1 = restTemplate.exchange(
                "/api/v1/pos/sync/context",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                DeviceContextDto.class);
        assertThat(contextResponse1.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Block the device
        HttpEntity<Void> blockRequest = new HttpEntity<>(adminHeaders);
        ResponseEntity<Void> blockResponse = restTemplate.exchange(
                "/api/v1/admin/devices/" + deviceId + "/block",
                HttpMethod.PUT,
                blockRequest,
                Void.class);
        assertThat(blockResponse.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NO_CONTENT);

        // Try to sync with blocked device - should fail
        ResponseEntity<DeviceContextDto> contextResponse2 = restTemplate.exchange(
                "/api/v1/pos/sync/context",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                DeviceContextDto.class);
        assertThat(contextResponse2.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void testListAllDevices() {
        // Admin lists all devices
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(adminHeaders);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/admin/devices",
                HttpMethod.GET,
                requestEntity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void testGenerateCodeWithCustomValidity() {
        // Generate code with 48 hours validity
        GenerateCodeRequest codeRequest = new GenerateCodeRequest(
                testStoreId,
                "Long Validity POS",
                48);

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        HttpEntity<GenerateCodeRequest> requestEntity = new HttpEntity<>(codeRequest, adminHeaders);

        ResponseEntity<GeneratedCodeDto> response = restTemplate.exchange(
                "/api/v1/admin/devices/generate-code",
                HttpMethod.POST,
                requestEntity,
                GeneratedCodeDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isNotBlank();
        assertThat(response.getBody().posName()).isEqualTo("Long Validity POS");
    }

    @Test
    @Disabled("TestSecurityConfig permits all requests - security checks are disabled in test environment")
    void testUnauthorizedUserCannotGenerateCode() {
        // Try to generate code without authentication
        GenerateCodeRequest codeRequest = new GenerateCodeRequest(
                testStoreId,
                "Unauthorized POS",
                24);

        ResponseEntity<GeneratedCodeDto> response = restTemplate.postForEntity(
                "/api/v1/admin/devices/generate-code",
                codeRequest,
                GeneratedCodeDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
