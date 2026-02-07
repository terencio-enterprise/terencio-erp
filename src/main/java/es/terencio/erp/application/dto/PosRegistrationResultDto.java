package es.terencio.erp.application.dto;

import java.util.UUID;

public record PosRegistrationResultDto(
    UUID storeId,
    String storeName,
    UUID deviceId,
    String serialCode,
    String licenseKey
) {}
