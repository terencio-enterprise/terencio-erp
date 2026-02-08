package es.terencio.erp.devices.application.dto;

import java.util.UUID;

public record SetupResultDto(
    UUID storeId,
    String storeName,
    UUID deviceId,
    String serialCode,
    String licenseKey
) {}