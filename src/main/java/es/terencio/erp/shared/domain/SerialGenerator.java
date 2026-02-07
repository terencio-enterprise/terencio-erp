package es.terencio.erp.shared.domain;

/**
 * Domain service for generating unique serial codes.
 */
public final class SerialGenerator {

    private SerialGenerator() {
        // Utility class
    }

    /**
     * Generates a unique serial code for a POS device.
     * Format: POS-{STORE_CODE}-{SEQUENCE}
     * 
     * @param storeCode the store code (e.g., "MAD", "BCN")
     * @return generated serial code
     */
    public static String generate(String storeCode) {
        // In a real implementation, this would query the database
        // to get the next sequence number for the store
        // For now, we use a timestamp-based approach
        long timestamp = System.currentTimeMillis() % 1000000;
        return String.format("POS-%s-%06d", storeCode.toUpperCase(), timestamp);
    }
}
