package es.terencio.erp.auth.infrastructure.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.access.secret}")
    private String accessSecret;

    @Value("${app.jwt.access.expiration-ms}")
    private long accessExpirationMs;

    @Value("${app.jwt.refresh.secret}")
    private String refreshSecret;

    @Value("${app.jwt.refresh.expiration-ms}")
    private long refreshExpirationMs;

    private SecretKey getSignInKey(String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ==========================================
    // ACCESS TOKEN
    // ==========================================
    public String generateAccessToken(Authentication authentication) {
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
        return buildToken(userPrincipal, accessExpirationMs, getSignInKey(accessSecret));
    }

    public String getUsernameFromAccessToken(String token) {
        return getUsernameFromToken(token, getSignInKey(accessSecret));
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token, getSignInKey(accessSecret));
    }

    // ==========================================
    // REFRESH TOKEN
    // ==========================================
    public String generateRefreshToken(Authentication authentication) {
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
        return buildToken(userPrincipal, refreshExpirationMs, getSignInKey(refreshSecret));
    }

    public String getUsernameFromRefreshToken(String token) {
        return getUsernameFromToken(token, getSignInKey(refreshSecret));
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, getSignInKey(refreshSecret));
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================
    private String buildToken(CustomUserDetails userPrincipal, long expiration, SecretKey key) {
        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("role", userPrincipal.getAuthorities().toString())
                .claim("storeId", userPrincipal.getStoreId())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + expiration))
                .signWith(key)
                .compact();
    }

    private String getUsernameFromToken(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    private boolean validateToken(String token, SecretKey key) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Log error
        }
        return false;
    }
}
