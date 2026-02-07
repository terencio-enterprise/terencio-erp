package es.terencio.erp.pos.application.dto;

import java.util.List;

import es.terencio.erp.users.application.dto.UserDto;

/**
 * Response DTO for POS registration preview.
 * Contains store and user information for initial sync.
 */
public record PosRegistrationPreviewDto(
        String posId, // Generated/Pre-assigned logical ID
        String posName, // "Caja Principal 01"
        String storeId, // UUID
        String storeName,
        List<UserDto> users // Initial sync of users
) {
}