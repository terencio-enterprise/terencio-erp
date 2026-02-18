package es.terencio.erp.devices.application.dto;

import java.time.Instant;
import java.util.UUID;

public record DeviceDto(
    UUID id,
    UUID storeId,
    String storeName,
    String serialCode,
    String hardwareId,
    String status,
    String versionApp,
    Instant lastSyncAt,
    Instant createdAt
) {}
