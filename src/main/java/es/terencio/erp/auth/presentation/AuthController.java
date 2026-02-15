package es.terencio.erp.auth.presentation;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.auth.application.dto.LoginRequest;
import es.terencio.erp.auth.application.dto.LoginResponse;
import es.terencio.erp.auth.application.dto.UserInfoDto;
import es.terencio.erp.auth.infrastructure.security.CustomUserDetails;
import es.terencio.erp.auth.infrastructure.security.JwtTokenProvider;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*", allowCredentials = "true")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Login endpoint - authenticates user and returns JWT in HTTP-only cookie.
     * Also returns user info in response body.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");

        // Create HTTP-only cookie for JWT
        ResponseCookie jwtCookie = ResponseCookie.from("JWT-TOKEN", jwt)
                .httpOnly(true)
                .secure(false) // Set to true in production with HTTPS
                .path("/")
                .maxAge(24 * 60 * 60) // 24 hours
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(new LoginResponse(jwt, authentication.getName(), role));
    }

    /**
     * Logout endpoint - clears JWT cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie jwtCookie = ResponseCookie.from("JWT-TOKEN", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .build();
    }

    /**
     * Get current authenticated user information.
     */
    @GetMapping("/me")
    public ResponseEntity<UserInfoDto> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        UserInfoDto userInfo = new UserInfoDto(
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getFullName(),
                userDetails.getRole(),
                userDetails.getStoreId(),
                userDetails.isEnabled());

        return ResponseEntity.ok(userInfo);
    }
}