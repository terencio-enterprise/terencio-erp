package es.terencio.erp.devices.application.dto;

import java.util.List;
import es.terencio.erp.users.application.dto.UserDto;

public record SetupPreviewDto(
    String posId,
    String posName,
    String storeId,
    String storeName,
    List<UserDto> users
) {}