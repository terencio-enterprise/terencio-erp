package es.terencio.erp.employees.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import es.terencio.erp.auth.domain.model.AccessGrant;
import es.terencio.erp.employees.application.dto.EmployeeDto;
import es.terencio.erp.employees.application.dto.EmployeeSyncDto;

public interface EmployeePort {
    List<EmployeeDto> findAll();

    Optional<EmployeeDto> findById(Long id);

    Optional<EmployeeDto> findByUsername(String username);

    List<EmployeeDto> findByStoreId(UUID storeId);

    List<EmployeeSyncDto> findSyncDataByStoreId(UUID storeId);

    List<AccessGrant> findAccessGrants(Long employeeId);

    Long save(String username, String fullName, String role, String pinHash, String passwordHash, UUID companyId,
            UUID storeId, String permissionsJson);

    void update(Long id, String fullName, String role, UUID storeId, boolean isActive, String permissionsJson);

    void syncAccessGrants(Long EmployeeId, String role, UUID companyId, UUID storeId);

    void updatePin(Long id, String newPinHash);

    void updatePassword(Long id, String newPasswordHash);

    void updateLastActiveContext(Long id, UUID companyId, UUID storeId);
}
