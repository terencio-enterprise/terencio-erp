package es.terencio.erp.devices.application.dto;

import java.util.List;

import es.terencio.erp.employees.application.dto.EmployeeDto;

public record SetupPreviewDto(
        String posId,
        String posName,
        String storeId,
        String storeName,
        List<EmployeeDto> users) {
}
