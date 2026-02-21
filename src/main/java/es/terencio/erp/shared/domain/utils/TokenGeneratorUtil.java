package es.terencio.erp.shared.domain.utils;

import java.security.SecureRandom;
import java.util.Base64;

public class TokenGeneratorUtil {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
