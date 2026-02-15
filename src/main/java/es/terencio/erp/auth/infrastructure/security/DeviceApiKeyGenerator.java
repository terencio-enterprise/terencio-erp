package es.terencio.erp.auth.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Handles device API key generation and validation using device-specific
 * secrets
 * combined with a system-wide secret for additional security.
 * 
 * API Key Format: {device_id}.{version}.{signature}
 * Signature = HMAC-SHA256(device_id + version, device_secret + system_secret)
 */
@Component
public class DeviceApiKeyGenerator {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int DEVICE_SECRET_LENGTH = 32; // bytes
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${security.device.api-secret}")
    private String systemSecret;

    /**
     * Generates a new cryptographically secure device secret (to be stored hashed
     * in DB)
     */
    public String generateDeviceSecret() {
        byte[] bytes = new byte[DEVICE_SECRET_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Generates an API key that the device will use for authentication
     * 
     * @param deviceId     The unique device identifier
     * @param deviceSecret The device-specific secret (plaintext, from DB or newly
     *                     generated)
     * @param version      The API key version (for rotation)
     * @return Complete API key in format: deviceId.version.signature
     */
    public String generateApiKey(UUID deviceId, String deviceSecret, int version) {
        String signature = computeSignature(deviceId.toString(), deviceSecret, version);
        return String.format("%s.%d.%s", deviceId, version, signature);
    }

    /**
     * Validates an API key against the stored device secret
     * 
     * @param apiKey          The full API key from the request header
     * @param deviceId        Expected device ID
     * @param deviceSecret    The device secret from database (plaintext)
     * @param expectedVersion The current API key version from database
     * @return true if API key is valid
     */
    public boolean validateApiKey(String apiKey, UUID deviceId, String deviceSecret, int expectedVersion) {
        try {
            String[] parts = apiKey.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String keyDeviceId = parts[0];
            int keyVersion = Integer.parseInt(parts[1]);
            String keySignature = parts[2];

            // Verify device ID matches
            if (!deviceId.toString().equals(keyDeviceId)) {
                return false;
            }

            // Verify version matches (prevents use of old rotated keys)
            if (keyVersion != expectedVersion) {
                return false;
            }

            // Recompute signature and compare
            String expectedSignature = computeSignature(keyDeviceId, deviceSecret, keyVersion);
            return constantTimeEquals(keySignature, expectedSignature);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts device ID from API key without full validation
     * Useful for looking up device before validation
     */
    public UUID extractDeviceId(String apiKey) {
        try {
            String[] parts = apiKey.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            return UUID.fromString(parts[0]);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Computes HMAC signature using device secret + system secret
     */
    private String computeSignature(String deviceId, String deviceSecret, int version) {
        try {
            // Combine secrets for additional security layer
            String combinedSecret = deviceSecret + systemSecret;
            String message = deviceId + ":" + version;

            Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(
                    combinedSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM);
            hmac.init(secretKey);

            byte[] signatureBytes = hmac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to compute API key signature", e);
        }
    }

    /**
     * Constant-time string comparison to prevent timing attacks
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }
}
