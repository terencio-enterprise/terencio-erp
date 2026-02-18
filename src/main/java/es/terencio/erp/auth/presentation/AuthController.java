package es.terencio.erp.auth.presentation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.auth.application.dto.EmployeeInfoDto;
import es.terencio.erp.auth.application.dto.LoginRequest;
import es.terencio.erp.auth.application.dto.LoginResponse;
import es.terencio.erp.auth.infrastructure.security.CustomUserDetails;
import es.terencio.erp.auth.infrastructure.security.JwtTokenProvider;
import es.terencio.erp.employees.application.port.out.EmployeePort;
import es.terencio.erp.shared.presentation.ApiError;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and session management endpoints")
public class AuthController {

        private final AuthenticationManager authenticationManager;
        private final JwtTokenProvider tokenProvider;
        private final UserDetailsService userDetailsService;

        // ACCESS TOKEN COOKIE CONFIG
        @Value("${app.jwt.access.cookie.name}")
        private String accessCookieName;

        @Value("${app.jwt.access.cookie.path}")
        private String accessCookiePath;

        @Value("${app.jwt.access.cookie.http-only}")
        private boolean accessCookieHttpOnly;

        @Value("${app.jwt.access.cookie.secure}")
        private boolean accessCookieSecure;

        @Value("${app.jwt.access.cookie.same-site}")
        private String accessCookieSameSite;

        @Value("${app.jwt.access.expiration-ms}")
        private long accessExpirationMs;

        // REFRESH TOKEN COOKIE CONFIG
        @Value("${app.jwt.refresh.cookie.name}")
        private String refreshCookieName;

        @Value("${app.jwt.refresh.cookie.path}")
        private String refreshCookiePath;

        @Value("${app.jwt.refresh.cookie.http-only}")
        private boolean refreshCookieHttpOnly;

        @Value("${app.jwt.refresh.cookie.secure}")
        private boolean refreshCookieSecure;

        @Value("${app.jwt.refresh.cookie.same-site}")
        private String refreshCookieSameSite;

        @Value("${app.jwt.refresh.expiration-ms}")
        private long refreshExpirationMs;

        private final es.terencio.erp.organization.application.service.OrganizationTreeService organizationTreeService;
        private final EmployeePort employeePort;

        public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider,
                        UserDetailsService userDetailsService,
                        es.terencio.erp.organization.application.service.OrganizationTreeService organizationTreeService,
                        EmployeePort employeePort) {
                this.authenticationManager = authenticationManager;
                this.tokenProvider = tokenProvider;
                this.userDetailsService = userDetailsService;
                this.organizationTreeService = organizationTreeService;
                this.employeePort = employeePort;
        }

        /**
         * Login endpoint - authenticates user, sets Access and Refresh tokens in
         * cookies.
         */
        @PostMapping("/login")
        @Operation(summary = "Authenticate user", description = "Authenticates with username/password and sets access and refresh token cookies")
        public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                String accessToken = tokenProvider.generateAccessToken(authentication);
                String refreshToken = tokenProvider.generateRefreshToken(authentication);

                // Create Access Token Cookie
                ResponseCookie accessCookie = ResponseCookie
                                .from(java.util.Objects.requireNonNull(accessCookieName), accessToken)
                                .httpOnly(accessCookieHttpOnly)
                                .secure(accessCookieSecure)
                                .path(java.util.Objects.requireNonNull(accessCookiePath))
                                .maxAge(accessExpirationMs / 1000)
                                .sameSite(java.util.Objects.requireNonNull(accessCookieSameSite))
                                .build();

                // Create Refresh Token Cookie
                ResponseCookie refreshCookie = ResponseCookie
                                .from(java.util.Objects.requireNonNull(refreshCookieName), refreshToken)
                                .httpOnly(refreshCookieHttpOnly)
                                .secure(refreshCookieSecure)
                                .path(java.util.Objects.requireNonNull(refreshCookiePath))
                                .maxAge(refreshExpirationMs / 1000)
                                .sameSite(java.util.Objects.requireNonNull(refreshCookieSameSite))
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                                .body(ApiResponse.success("Login successful",
                                                new LoginResponse(accessToken, authentication.getName())));
        }

        /**
         * Refresh Token endpoint - generates new Access Token using valid Refresh Token
         * from cookie.
         */
        @PostMapping("/refresh")
        @Operation(summary = "Refresh access token", description = "Generates a new access token using the refresh token cookie")
        public ResponseEntity<?> refresh(
                        @Parameter(description = "Refresh token from cookie") @CookieValue(name = "${app.jwt.refresh.cookie.name}", required = false) String refreshToken) {
                if (refreshToken != null && tokenProvider.validateRefreshToken(refreshToken)) {
                        String username = tokenProvider.getUsernameFromRefreshToken(refreshToken);

                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        if (!userDetails.isEnabled()) {
                                ApiError error = new ApiError("ACCOUNT_DISABLED", "User account is disabled", null);
                                return ResponseEntity.status(401)
                                                .body(ApiResponse.error("User account is disabled", error));
                        }

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());

                        String newAccessToken = tokenProvider.generateAccessToken(authentication);

                        // Create new Access Token Cookie
                        ResponseCookie newAccessCookie = ResponseCookie
                                        .from(java.util.Objects.requireNonNull(accessCookieName), newAccessToken)
                                        .httpOnly(accessCookieHttpOnly)
                                        .secure(accessCookieSecure)
                                        .path(java.util.Objects.requireNonNull(accessCookiePath))
                                        .maxAge(accessExpirationMs / 1000)
                                        .sameSite(java.util.Objects.requireNonNull(accessCookieSameSite))
                                        .build();

                        return ResponseEntity.ok()
                                        .header(HttpHeaders.SET_COOKIE, newAccessCookie.toString())
                                        .body(ApiResponse.success("Token refreshed successfully",
                                                        new LoginResponse(newAccessToken, userDetails.getUsername())));
                }

                ApiError error = new ApiError("INVALID_TOKEN", "Invalid Refresh Token", null);
                return ResponseEntity.status(401)
                                .body(ApiResponse.error("Invalid Refresh Token", error));
        }

        /**
         * Logout endpoint - clears Access and Refresh Token cookies.
         */
        @PostMapping("/logout")
        @Operation(summary = "Logout user", description = "Clears authentication cookies for current session")
        public ResponseEntity<ApiResponse<Void>> logout() {
                // Clear Access Token Cookie
                ResponseCookie accessCookie = ResponseCookie
                                .from(java.util.Objects.requireNonNull(accessCookieName), "")
                                .httpOnly(accessCookieHttpOnly)
                                .secure(accessCookieSecure)
                                .path(java.util.Objects.requireNonNull(accessCookiePath))
                                .maxAge(0)
                                .sameSite(java.util.Objects.requireNonNull(accessCookieSameSite))
                                .build();

                // Clear Refresh Token Cookie
                ResponseCookie refreshCookie = ResponseCookie
                                .from(java.util.Objects.requireNonNull(refreshCookieName), "")
                                .httpOnly(refreshCookieHttpOnly)
                                .secure(refreshCookieSecure)
                                .path(java.util.Objects.requireNonNull(refreshCookiePath))
                                .maxAge(0)
                                .sameSite(java.util.Objects.requireNonNull(refreshCookieSameSite))
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                                .body(ApiResponse.success("Logout successful"));
        }

        /**
         * Get current authenticated user information.
         */
        @GetMapping("/me")
        @Operation(summary = "Get current user", description = "Returns information for the currently authenticated user")
        public ResponseEntity<ApiResponse<EmployeeInfoDto>> getCurrentUser(
                        @Parameter(description = "Authenticated principal") @AuthenticationPrincipal CustomUserDetails userDetails) {
                if (userDetails == null) {
                        return ResponseEntity.status(401).build();
                }

                es.terencio.erp.employees.application.dto.EmployeeDto employee = employeePort
                                .findById(userDetails.getId())
                                .orElseThrow(() -> new RuntimeException("Employee not found"));

                EmployeeInfoDto userInfo = new EmployeeInfoDto(
                                userDetails.getId(),
                                userDetails.getUsername(),
                                userDetails.getFullName(),
                                userDetails.isEnabled(),
                                employee.lastActiveCompanyId(),
                                employee.lastActiveStoreId(),
                                organizationTreeService.getCompanyTreeForEmployee(userDetails.getId()));

                return ResponseEntity.ok(ApiResponse.success("User info fetched successfully", userInfo));
        }
}
