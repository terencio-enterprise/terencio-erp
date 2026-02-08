package es.terencio.erp.devices.application.dto;

import java.time.Instant;

public record GeneratedCodeDto(
    String code,
    String posName,
    Instant expiresAt
) {}