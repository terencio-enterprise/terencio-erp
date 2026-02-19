package es.terencio.erp.auth.infrastructure.config.security.jwt;

import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import es.terencio.erp.auth.infrastructure.config.security.CustomUserDetails;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.access.secret}") private String accessSecret;
    @Value("${app.jwt.access.expiration-ms}") private long accessExpirationMs;
    @Value("${app.jwt.refresh.secret}") private String refreshSecret;
    @Value("${app.jwt.refresh.expiration-ms}") private long refreshExpirationMs;

    private SecretKey getSignInKey(String secret) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateAccessToken(Authentication authentication) {
        return buildToken((CustomUserDetails) authentication.getPrincipal(), accessExpirationMs, getSignInKey(accessSecret));
    }

    public String getUsernameFromAccessToken(String token) {
        return getUsernameFromToken(token, getSignInKey(accessSecret));
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token, getSignInKey(accessSecret));
    }

    public String generateRefreshToken(Authentication authentication) {
        return buildToken((CustomUserDetails) authentication.getPrincipal(), refreshExpirationMs, getSignInKey(refreshSecret));
    }

    public String getUsernameFromRefreshToken(String token) {
        return getUsernameFromToken(token, getSignInKey(refreshSecret));
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, getSignInKey(refreshSecret));
    }

    private String buildToken(CustomUserDetails userPrincipal, long expiration, SecretKey key) {
        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("role", userPrincipal.getAuthorities().toString())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + expiration))
                .signWith(key)
                .compact();
    }

    private String getUsernameFromToken(String token, SecretKey key) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }

    private boolean validateToken(String token, SecretKey key) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
