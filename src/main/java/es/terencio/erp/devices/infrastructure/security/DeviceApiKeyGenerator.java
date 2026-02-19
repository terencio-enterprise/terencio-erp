package es.terencio.erp.devices.infrastructure.security;

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

@Component
public class DeviceApiKeyGenerator {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int DEVICE_SECRET_LENGTH = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${security.device.api-secret}")
    private String systemSecret;

    public String generateDeviceSecret() {
        byte[] bytes = new byte[DEVICE_SECRET_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String generateApiKey(UUID deviceId, String deviceSecret, int version) {
        String signature = computeSignature(deviceId.toString(), deviceSecret, version);
        return String.format("%s.%d.%s", deviceId, version, signature);
    }

    public boolean validateApiKey(String apiKey, UUID deviceId, String deviceSecret, int expectedVersion) {
        try {
            String[] parts = apiKey.split("\\.");
            if (parts.length != 3) return false;

            String keyDeviceId = parts[0];
            int keyVersion = Integer.parseInt(parts[1]);
            String keySignature = parts[2];

            if (!deviceId.toString().equals(keyDeviceId) || keyVersion != expectedVersion) {
                return false;
            }

            String expectedSignature = computeSignature(keyDeviceId, deviceSecret, keyVersion);
            return constantTimeEquals(keySignature, expectedSignature);
        } catch (Exception e) {
            return false;
        }
    }

    public UUID extractDeviceId(String apiKey) {
        try {
            String[] parts = apiKey.split("\\.");
            if (parts.length != 3) return null;
            return UUID.fromString(parts[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private String computeSignature(String deviceId, String deviceSecret, int version) {
        try {
            String combinedSecret = deviceSecret + systemSecret;
            String message = deviceId + ":" + version;

            Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(combinedSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            hmac.init(secretKey);

            byte[] signatureBytes = hmac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to compute API key signature", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }
}
