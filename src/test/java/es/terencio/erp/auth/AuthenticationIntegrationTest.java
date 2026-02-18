package es.terencio.erp.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
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
import es.terencio.erp.auth.application.dto.EmployeeInfoDto;
import es.terencio.erp.auth.application.dto.LoginRequest;
import es.terencio.erp.auth.application.dto.LoginResponse;
import es.terencio.erp.shared.presentation.ApiResponse;

/**
 * Integration test for Authentication endpoints.
 * Tests the complete user authentication flow:
 * 1. User login with credentials
 * 2. Receive Access and Refresh tokens in cookies
 * 3. Access protected endpoint with Access Token cookie
 * 4. Get current user info
 * 5. Logout and verify cookies are cleared
 */
class AuthenticationIntegrationTest extends AbstractIntegrationTest {

        @Autowired
        private TestRestTemplate restTemplate;

        @Autowired
        private JdbcClient jdbcClient;

        @Autowired
        private PasswordEncoder passwordEncoder;

        private UUID testCompanyId;
        private UUID testOrganizationId;
        private UUID testStoreId;
        private Long adminEmployeeId;
        private Long cashierEmployeeId;
        private Long managerEmployeeId;

        @BeforeEach
        void setUp() {
                cleanDatabase();

                // Create test organization
                testOrganizationId = UUID.randomUUID();
                jdbcClient.sql("""
                                INSERT INTO organizations (id, name, slug, subscription_plan)
                                VALUES (:id, 'Test Organization', 'test-organization', 'STANDARD')
                                """)
                                .param("id", testOrganizationId)
                                .update();

                // Create test company
                testCompanyId = UUID.randomUUID();
                jdbcClient.sql("""
                                INSERT INTO companies (id, organization_id, name, slug, tax_id, is_active)
                                VALUES (:id, :organizationId, 'Test Company', 'test-company', 'B11111111', TRUE)
                                """)
                                .param("id", testCompanyId)
                                .param("organizationId", testOrganizationId)
                                .update();

                // Create test store
                testStoreId = UUID.randomUUID();
                jdbcClient.sql("""
                                INSERT INTO stores (id, company_id, code, name, slug, address, is_active)
                                VALUES (:id, :companyId, 'TEST-STORE', 'Test Store', 'test-store', 'Test Address', TRUE)
                                """)
                                .param("id", testStoreId)
                                .param("companyId", testCompanyId)
                                .update();

                // Create admin user
                String adminPassword = passwordEncoder.encode("admin123");
                adminEmployeeId = jdbcClient
                                .sql("""
                                                                                INSERT INTO employees (username, full_name, role, pin_hash, password_hash,
                                                                                        organization_id, store_id, permissions_json, is_active)
                                                VALUES ('admin', 'Admin User', 'ADMIN', 'pin123', :password,
                                                                                        (SELECT organization_id FROM companies WHERE id = :companyId), :storeId, '[]', TRUE)
                                                RETURNING id
                                                """)
                                .param("password", adminPassword)
                                .param("companyId", testCompanyId)
                                .param("storeId", testStoreId)
                                .query(Long.class)
                                .single();

                // Create cashier user
                String cashierPassword = passwordEncoder.encode("cashier123");
                cashierEmployeeId = jdbcClient
                                .sql("""
                                                                                INSERT INTO employees (username, full_name, role, pin_hash, password_hash,
                                                                                        organization_id, store_id, permissions_json, is_active)
                                                VALUES ('cashier', 'Cashier User', 'CASHIER', 'pin456', :password,
                                                                                        (SELECT organization_id FROM companies WHERE id = :companyId), :storeId, '[]', TRUE)
                                                RETURNING id
                                                """)
                                .param("password", cashierPassword)
                                .param("companyId", testCompanyId)
                                .param("storeId", testStoreId)
                                .query(Long.class)
                                .single();

                // Create manager user
                String managerPassword = passwordEncoder.encode("manager123");
                managerEmployeeId = jdbcClient
                                .sql("""
                                                                                INSERT INTO employees (username, full_name, role, pin_hash, password_hash,
                                                                                        organization_id, store_id, permissions_json, is_active)
                                                VALUES ('manager', 'Manager User', 'MANAGER', 'pin789', :password,
                                                                                        (SELECT organization_id FROM companies WHERE id = :companyId), :storeId, '[]', TRUE)
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
                ResponseEntity<ApiResponse<LoginResponse>> response = restTemplate.exchange(
                                "/api/v1/auth/login",
                                HttpMethod.POST,
                                new HttpEntity<>(loginRequest),
                                new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {
                                });

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isTrue();

                LoginResponse loginResponse = response.getBody().getData();
                assertThat(loginResponse.token()).isNotBlank();
                assertThat(loginResponse.username()).isEqualTo("admin");

                // Verify Access Token cookie is set
                List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
                assertThat(cookies).isNotNull();
                assertThat(cookies).anyMatch(cookie -> cookie.startsWith("ACCESS_TOKEN="));
                assertThat(cookies).anyMatch(cookie -> cookie.contains("HttpOnly"));
                assertThat(cookies).anyMatch(cookie -> cookie.contains("Path=/"));
                assertThat(cookies).anyMatch(cookie -> cookie.contains("Max-Age="));

                // Verify Refresh Token cookie is set
                assertThat(cookies).anyMatch(cookie -> cookie.startsWith("REFRESH_TOKEN="));
                assertThat(cookies).anyMatch(cookie -> cookie.contains("Path=/api/v1/auth/refresh"));
        }

        @Test
        void testLogout() {
                // Given - Login first
                LoginRequest loginRequest = new LoginRequest("admin", "admin123");
                ResponseEntity<ApiResponse<LoginResponse>> loginResponse = restTemplate.exchange(
                                "/api/v1/auth/login",
                                HttpMethod.POST,
                                new HttpEntity<>(loginRequest),
                                new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {
                                });

                assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

                // When - Logout
                ResponseEntity<ApiResponse<Void>> logoutResponse = restTemplate.exchange(
                                "/api/v1/auth/logout",
                                HttpMethod.POST,
                                null,
                                new ParameterizedTypeReference<ApiResponse<Void>>() {
                                });

                // Then
                assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(logoutResponse.getBody()).isNotNull();
                assertThat(logoutResponse.getBody().isSuccess()).isTrue();

                // Verify cookies are cleared
                List<String> cookies = logoutResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
                assertThat(cookies).isNotNull();
                assertThat(cookies)
                                .anyMatch(cookie -> cookie.startsWith("ACCESS_TOKEN=;")
                                                || cookie.startsWith("ACCESS_TOKEN=\"\";"));
                assertThat(cookies)
                                .anyMatch(cookie -> cookie.startsWith("REFRESH_TOKEN=;")
                                                || cookie.startsWith("REFRESH_TOKEN=\"\";"));
                assertThat(cookies).anyMatch(cookie -> cookie.contains("Max-Age=0"));
        }

        @Test
        void testLoginWithInvalidCredentials() {
                // Given
                LoginRequest loginRequest = new LoginRequest("admin", "wrongpassword");

                // When
                ResponseEntity<ApiResponse<LoginResponse>> response = restTemplate.exchange(
                                "/api/v1/auth/login",
                                HttpMethod.POST,
                                new HttpEntity<>(loginRequest),
                                new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {
                                });

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void testLoginWithNonExistentUser() {
                // Given
                LoginRequest loginRequest = new LoginRequest("nonexistent", "password");

                // When
                ResponseEntity<ApiResponse<LoginResponse>> response = restTemplate.exchange(
                                "/api/v1/auth/login",
                                HttpMethod.POST,
                                new HttpEntity<>(loginRequest),
                                new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {
                                });

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void testGetCurrentUserWithAccessCookie() {
                // Given - Login first
                LoginRequest loginRequest = new LoginRequest("cashier", "cashier123");
                ResponseEntity<ApiResponse<LoginResponse>> loginResponse = restTemplate.exchange(
                                "/api/v1/auth/login",
                                HttpMethod.POST,
                                new HttpEntity<>(loginRequest),
                                new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {
                                });

                assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

                // Extract Access Token from cookie (simulating browser)
                String accessTokenCookie = loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                                .filter(c -> c.startsWith("ACCESS_TOKEN="))
                                .findFirst()
                                .orElseThrow();

                // When - Get current user info with Access Token cookie
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.COOKIE, accessTokenCookie);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<ApiResponse<EmployeeInfoDto>> response = restTemplate.exchange(
                                "/api/v1/auth/me",
                                HttpMethod.GET,
                                entity,
                                new ParameterizedTypeReference<ApiResponse<EmployeeInfoDto>>() {
                                });

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isTrue();

                EmployeeInfoDto userInfo = response.getBody().getData();
                assertThat(userInfo.id()).isEqualTo(cashierEmployeeId);
                assertThat(userInfo.username()).isEqualTo("cashier");
                assertThat(userInfo.companies()).isNotEmpty();
                assertThat(userInfo.companies().get(0).id()).isEqualTo(testCompanyId);
                // Last active context should be null as not set yet (unless I set it)
                assertThat(userInfo.lastCompanyId()).isNull();
                assertThat(userInfo.lastStoreId()).isNull();
        }

        @Test
        void testGetCurrentUserWithBearerToken() {
                // Given - Login first
                LoginRequest loginRequest = new LoginRequest("manager", "manager123");
                ResponseEntity<ApiResponse<LoginResponse>> loginResponse = restTemplate.exchange(
                                "/api/v1/auth/login",
                                HttpMethod.POST,
                                new HttpEntity<>(loginRequest),
                                new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {
                                });

                assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                String jwtToken = loginResponse.getBody().getData().token();

                // When - Get current user info with Bearer token
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(jwtToken);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<ApiResponse<EmployeeInfoDto>> response = restTemplate.exchange(
                                "/api/v1/auth/me",
                                HttpMethod.GET,
                                entity,
                                new ParameterizedTypeReference<ApiResponse<EmployeeInfoDto>>() {
                                });

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isTrue();

                EmployeeInfoDto userInfo = response.getBody().getData();
                assertThat(userInfo.id()).isEqualTo(managerEmployeeId);
                assertThat(userInfo.username()).isEqualTo("manager");
        }

        @Test
        void testGetCurrentUserWithoutToken() {
                // When - Try to access protected endpoint without token
                ResponseEntity<EmployeeInfoDto> response = restTemplate.getForEntity(
                                "/api/v1/auth/me",
                                EmployeeInfoDto.class);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void testMultipleUsersCanLoginSimultaneously() {
                // Login as admin
                LoginRequest adminLogin = new LoginRequest("admin", "admin123");
                ResponseEntity<ApiResponse<LoginResponse>> adminResponse = restTemplate.exchange(
                                "/api/v1/auth/login",
                                HttpMethod.POST,
                                new HttpEntity<>(adminLogin),
                                new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {
                                });
                assertThat(adminResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                String adminToken = adminResponse.getBody().getData().token();

                // Login as cashier
                LoginRequest cashierLogin = new LoginRequest("cashier", "cashier123");
                ResponseEntity<ApiResponse<LoginResponse>> cashierResponse = restTemplate.exchange(
                                "/api/v1/auth/login",
                                HttpMethod.POST,
                                new HttpEntity<>(cashierLogin),
                                new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {
                                });
                assertThat(cashierResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                String cashierToken = cashierResponse.getBody().getData().token();

                // Verify both tokens are different
                assertThat(adminToken).isNotEqualTo(cashierToken);

                // Verify both tokens work
                HttpHeaders adminHeaders = new HttpHeaders();
                adminHeaders.setBearerAuth(adminToken);
                ResponseEntity<ApiResponse<EmployeeInfoDto>> adminMeResponse = restTemplate.exchange(
                                "/api/v1/auth/me",
                                HttpMethod.GET,
                                new HttpEntity<>(adminHeaders),
                                new ParameterizedTypeReference<ApiResponse<EmployeeInfoDto>>() {
                                });
                assertThat(adminMeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(adminMeResponse.getBody().getData().username()).isEqualTo("admin");

                HttpHeaders cashierHeaders = new HttpHeaders();
                cashierHeaders.setBearerAuth(cashierToken);
                ResponseEntity<ApiResponse<EmployeeInfoDto>> cashierMeResponse = restTemplate.exchange(
                                "/api/v1/auth/me",
                                HttpMethod.GET,
                                new HttpEntity<>(cashierHeaders),
                                new ParameterizedTypeReference<ApiResponse<EmployeeInfoDto>>() {
                                });
                assertThat(cashierMeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(cashierMeResponse.getBody().getData().username()).isEqualTo("cashier");
        }

        @Test
        void testInactiveUserCannotLogin() {
                // Create inactive user
                String inactivePassword = passwordEncoder.encode("inactive123");
                jdbcClient
                                .sql("""
                                                                                INSERT INTO employees (username, full_name, role, pin_hash, password_hash,
                                                                                        organization_id, store_id, permissions_json, is_active)
                                                VALUES ('inactive', 'Inactive User', 'CASHIER', 'pin999', :password,
                                                                                        (SELECT organization_id FROM companies WHERE id = :companyId), :storeId, '[]', FALSE)
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
