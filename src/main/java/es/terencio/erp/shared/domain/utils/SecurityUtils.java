package es.terencio.erp.shared.domain.utils;

import java.security.SecureRandom;
import java.util.Base64;

public class SecurityUtils {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String generateSecureToken() {
        byte[] randomBytes = new byte[32]; // 256 bits of entropy
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
