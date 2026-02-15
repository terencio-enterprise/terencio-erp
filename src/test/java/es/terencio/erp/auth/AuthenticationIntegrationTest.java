package es.terencio.erp.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
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
import es.terencio.erp.auth.application.dto.UserInfoDto;

/**
 * Integration test for Authentication endpoints.
 * Tests the complete user authentication flow:
 * 1. User login with credentials
 * 2. Receive JWT token in cookie and response
 * 3. Access protected endpoint with JWT cookie
 * 4. Get current user info
 * 5. Logout and verify cookie is cleared
 */
class AuthenticationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID testCompanyId;
    private UUID testStoreId;
    private Long adminUserId;
    private Long cashierUserId;
    private Long managerUserId;

    @BeforeEach
    void setUp() {
        // Create test company
        testCompanyId = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO companies (id, code, name, tax_id, is_active)
                VALUES (:id, 'TEST-COMP', 'Test Company', 'B11111111', TRUE)
                """)
                .param("id", testCompanyId)
                .update();

        // Create test store
        testStoreId = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO stores (id, code, name, address, tax_id, is_active)
                VALUES (:id, 'TEST-STORE', 'Test Store', 'Test Address', 'B11111111', TRUE)
                """)
                .param("id", testStoreId)
                .update();

        // Create admin user
        String adminPassword = passwordEncoder.encode("admin123");
        adminUserId = jdbcClient.sql("""
                INSERT INTO users (username, full_name, role, pin_hash, password_hash,
                    company_id, store_id, permissions_json, is_active)
                VALUES ('admin', 'Admin User', 'ADMIN', 'pin123', :password,
                    :companyId, :storeId, '[]', TRUE)
                RETURNING id
                """)
                .param("password", adminPassword)
                .param("companyId", testCompanyId)
                .param("storeId", testStoreId)
                .query(Long.class)
                .single();

        // Create cashier user
        String cashierPassword = passwordEncoder.encode("cashier123");
        cashierUserId = jdbcClient.sql("""
                INSERT INTO users (username, full_name, role, pin_hash, password_hash,
                    company_id, store_id, permissions_json, is_active)
                VALUES ('cashier', 'Cashier User', 'CASHIER', 'pin456', :password,
                    :companyId, :storeId, '[]', TRUE)
                RETURNING id
                """)
                .param("password", cashierPassword)
                .param("companyId", testCompanyId)
                .param("storeId", testStoreId)
                .query(Long.class)
                .single();

        // Create manager user
        String managerPassword = passwordEncoder.encode("manager123");
        managerUserId = jdbcClient.sql("""
                INSERT INTO users (username, full_name, role, pin_hash, password_hash,
                    company_id, store_id, permissions_json, is_active)
                VALUES ('manager', 'Manager User', 'MANAGER', 'pin789', :password,
                    :companyId, :storeId, '[]', TRUE)
                RETURNING id
                """)
                .param("password", managerPassword)
                .param("companyId", testCompanyId)
                .param("storeId", testStoreId)
                .query(Long.class)
                .single();
    }

    @Test
    void testSuccessfulLogin() {
        // Given
        LoginRequest loginRequest = new LoginRequest("admin", "admin123");

        // When
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                loginRequest,
                LoginResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        LoginResponse loginResponse = response.getBody();
        assertThat(loginResponse.token()).isNotBlank();
        assertThat(loginResponse.username()).isEqualTo("admin");
        assertThat(loginResponse.role()).isEqualTo("ROLE_ADMIN");
        assertThat(loginResponse.type()).isEqualTo("Bearer");

        // Verify JWT cookie is set
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotNull();
        assertThat(cookies).anyMatch(cookie -> cookie.startsWith("JWT-TOKEN="));
        assertThat(cookies).anyMatch(cookie -> cookie.contains("HttpOnly"));
        assertThat(cookies).anyMatch(cookie -> cookie.contains("Path=/"));
        assertThat(cookies).anyMatch(cookie -> cookie.contains("Max-Age=86400")); // 24 hours
    }

    @Test
    void testLoginWithInvalidCredentials() {
        // Given
        LoginRequest loginRequest = new LoginRequest("admin", "wrongpassword");

        // When
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                loginRequest,
                LoginResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testLoginWithNonExistentUser() {
        // Given
        LoginRequest loginRequest = new LoginRequest("nonexistent", "password");

        // When
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                loginRequest,
                LoginResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testGetCurrentUserWithValidToken() {
        // Given - Login first
        LoginRequest loginRequest = new LoginRequest("cashier", "cashier123");
        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login",
                loginRequest,
                LoginResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String jwtToken = loginResponse.getBody().token();

        // When - Get current user info with JWT cookie
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "JWT-TOKEN=" + jwtToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<UserInfoDto> response = restTemplate.exchange(
                "/api/v1/auth/me",
                HttpMethod.GET,
                entity,
                UserInfoDto.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        UserInfoDto userInfo = response.getBody();
        assertThat(userInfo.id()).isEqualTo(cashierUserId);
        assertThat(userInfo.username()).isEqualTo("cashier");
        assertThat(userInfo.fullName()).isEqualTo("Cashier User");
        assertThat(userInfo.role()).isEqualTo("CASHIER");
        assertThat(userInfo.storeId()).isEqualTo(testStoreId);
        assertThat(userInfo.isActive()).isTrue();
    }

    @Test
    void testGetCurrentUserWithBearerToken() {
        // Given - Login first
        LoginRequest loginRequest = new LoginRequest("manager", "manager123");
        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login",
                loginRequest,
                LoginResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String jwtToken = loginResponse.getBody().token();

        // When - Get current user info with Bearer token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<UserInfoDto> response = restTemplate.exchange(
                "/api/v1/auth/me",
                HttpMethod.GET,
                entity,
                UserInfoDto.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        UserInfoDto userInfo = response.getBody();
        assertThat(userInfo.id()).isEqualTo(managerUserId);
        assertThat(userInfo.username()).isEqualTo("manager");
        assertThat(userInfo.role()).isEqualTo("MANAGER");
    }

    @Test
    void testGetCurrentUserWithoutToken() {
        // When - Try to access protected endpoint without token
        ResponseEntity<UserInfoDto> response = restTemplate.getForEntity(
                "/api/v1/auth/me",
                UserInfoDto.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testLogout() {
        // Given - Login first
        LoginRequest loginRequest = new LoginRequest("admin", "admin123");
        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login",
                loginRequest,
                LoginResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String jwtToken = loginResponse.getBody().token();

        // When - Logout
        ResponseEntity<Void> logoutResponse = restTemplate.postForEntity(
                "/api/v1/auth/logout",
                null,
                Void.class);

        // Then
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify JWT cookie is cleared
        List<String> cookies = logoutResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotNull();
        assertThat(cookies)
                .anyMatch(cookie -> cookie.startsWith("JWT-TOKEN=;") || cookie.startsWith("JWT-TOKEN=\"\";"));
        assertThat(cookies).anyMatch(cookie -> cookie.contains("Max-Age=0"));
    }

    @Test
    void testMultipleUsersCanLoginSimultaneously() {
        // Login as admin
        LoginRequest adminLogin = new LoginRequest("admin", "admin123");
        ResponseEntity<LoginResponse> adminResponse = restTemplate.postForEntity(
                "/api/v1/auth/login",
                adminLogin,
                LoginResponse.class);
        assertThat(adminResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String adminToken = adminResponse.getBody().token();

        // Login as cashier
        LoginRequest cashierLogin = new LoginRequest("cashier", "cashier123");
        ResponseEntity<LoginResponse> cashierResponse = restTemplate.postForEntity(
                "/api/v1/auth/login",
                cashierLogin,
                LoginResponse.class);
        assertThat(cashierResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String cashierToken = cashierResponse.getBody().token();

        // Verify both tokens are different
        assertThat(adminToken).isNotEqualTo(cashierToken);

        // Verify both tokens work
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        ResponseEntity<UserInfoDto> adminMeResponse = restTemplate.exchange(
                "/api/v1/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                UserInfoDto.class);
        assertThat(adminMeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminMeResponse.getBody().username()).isEqualTo("admin");

        HttpHeaders cashierHeaders = new HttpHeaders();
        cashierHeaders.setBearerAuth(cashierToken);
        ResponseEntity<UserInfoDto> cashierMeResponse = restTemplate.exchange(
                "/api/v1/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(cashierHeaders),
                UserInfoDto.class);
        assertThat(cashierMeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cashierMeResponse.getBody().username()).isEqualTo("cashier");
    }

    @Test
    void testLoginValidationErrors() {
        // Test empty username
        LoginRequest emptyUsername = new LoginRequest("", "password");
        ResponseEntity<LoginResponse> response1 = restTemplate.postForEntity(
                "/api/v1/auth/login",
                emptyUsername,
                LoginResponse.class);
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Test empty password
        LoginRequest emptyPassword = new LoginRequest("admin", "");
        ResponseEntity<LoginResponse> response2 = restTemplate.postForEntity(
                "/api/v1/auth/login",
                emptyPassword,
                LoginResponse.class);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testInactiveUserCannotLogin() {
        // Create inactive user
        String inactivePassword = passwordEncoder.encode("inactive123");
        jdbcClient.sql("""
                INSERT INTO users (username, full_name, role, pin_hash, password_hash,
                    company_id, store_id, permissions_json, is_active)
                VALUES ('inactive', 'Inactive User', 'CASHIER', 'pin999', :password,
                    :companyId, :storeId, '[]', FALSE)
                """)
                .param("password", inactivePassword)
                .param("companyId", testCompanyId)
                .param("storeId", testStoreId)
                .update();

        // Try to login as inactive user
        LoginRequest loginRequest = new LoginRequest("inactive", "inactive123");
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                loginRequest,
                LoginResponse.class);

        // Should be unauthorized (Spring Security disables inactive users)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
