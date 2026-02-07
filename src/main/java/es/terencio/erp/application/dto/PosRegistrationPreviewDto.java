package es.terencio.erp.application.dto;

import java.util.List;

public record PosRegistrationPreviewDto(
    String posId,       // Generated/Pre-assigned logical ID
    String posName,     // "Caja Principal 01"
    String storeId,     // UUID
    String storeName,
    String deviceId,    // Returned for confirmation
    List<UserDto> users // Initial sync of users
) {}