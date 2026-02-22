package es.terencio.erp.auth.infrastructure.in.web;

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

import es.terencio.erp.auth.application.dto.AuthDtos.EmployeeInfoDto;
import es.terencio.erp.auth.application.dto.AuthDtos.LoginRequest;
import es.terencio.erp.auth.application.dto.AuthDtos.LoginResponse;
import es.terencio.erp.auth.application.service.PermissionService;
import es.terencio.erp.auth.infrastructure.config.security.CustomUserDetails;
import es.terencio.erp.auth.infrastructure.config.security.jwt.JwtTokenProvider;
import es.terencio.erp.employees.application.dto.EmployeeDto;
import es.terencio.erp.employees.application.port.out.EmployeePort;
import es.terencio.erp.organization.application.port.in.OrganizationUseCase;
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
    private final OrganizationUseCase organizationUseCase;
    private final EmployeePort employeePort;
    private final PermissionService permissionService;

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

    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider,
            UserDetailsService userDetailsService, OrganizationUseCase organizationUseCase,
            EmployeePort employeePort, PermissionService permissionService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.organizationUseCase = organizationUseCase;
        this.employeePort = employeePort;
        this.permissionService = permissionService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        ResponseCookie accessCookie = createCookie(accessCookieName, accessToken, accessCookiePath, accessExpirationMs,
                accessCookieHttpOnly, accessCookieSecure, accessCookieSameSite);
        ResponseCookie refreshCookie = createCookie(refreshCookieName, refreshToken, refreshCookiePath,
                refreshExpirationMs, refreshCookieHttpOnly, refreshCookieSecure, refreshCookieSameSite);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success("Login successful",
                        new LoginResponse(accessToken, authentication.getName())));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<?> refresh(
            @CookieValue(name = "${app.jwt.refresh.cookie.name}", required = false) String refreshToken) {
        if (refreshToken != null && tokenProvider.validateRefreshToken(refreshToken)) {
            String username = tokenProvider.getUsernameFromRefreshToken(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!userDetails.isEnabled()) {
                return ResponseEntity.status(401).body(ApiResponse.error("User disabled",
                        new ApiError("ACCOUNT_DISABLED", "User account is disabled", null)));
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails,
                    null, userDetails.getAuthorities());
            String newAccessToken = tokenProvider.generateAccessToken(authentication);
            ResponseCookie newAccessCookie = createCookie(accessCookieName, newAccessToken, accessCookiePath,
                    accessExpirationMs, accessCookieHttpOnly, accessCookieSecure, accessCookieSameSite);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, newAccessCookie.toString())
                    .body(ApiResponse.success("Token refreshed successfully",
                            new LoginResponse(newAccessToken, userDetails.getUsername())));
        }
        return ResponseEntity.status(401).body(ApiResponse.error("Invalid Refresh Token",
                new ApiError("INVALID_TOKEN", "Invalid Refresh Token", null)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user")
    public ResponseEntity<ApiResponse<Void>> logout() {
        ResponseCookie accessCookie = createCookie(accessCookieName, "", accessCookiePath, 0, accessCookieHttpOnly,
                accessCookieSecure, accessCookieSameSite);
        ResponseCookie refreshCookie = createCookie(refreshCookieName, "", refreshCookiePath, 0, refreshCookieHttpOnly,
                refreshCookieSecure, refreshCookieSameSite);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success("Logout successful"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user")
    public ResponseEntity<ApiResponse<EmployeeInfoDto>> getCurrentUser(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null)
            return ResponseEntity.status(401).build();

        EmployeeDto employee = employeePort.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        EmployeeInfoDto userInfo = new EmployeeInfoDto(
                userDetails.getId(), userDetails.getUsername(), userDetails.getFullName(), userDetails.isEnabled(),
                employee.lastActiveCompanyId(), employee.lastActiveStoreId(),
                organizationUseCase.getTreeForEmployee(userDetails.getId()),
                permissionService.getPermissionMatrix(userDetails.getId()));

        return ResponseEntity.ok(ApiResponse.success("User info fetched successfully", userInfo));
    }

    private ResponseCookie createCookie(String name, String value, String path, long maxAge, boolean httpOnly,
            boolean secure, String sameSite) {
        return ResponseCookie.from(java.util.Objects.requireNonNull(name), value)
                .httpOnly(httpOnly).secure(secure).path(java.util.Objects.requireNonNull(path))
                .maxAge(maxAge / 1000).sameSite(java.util.Objects.requireNonNull(sameSite)).build();
    }
}
