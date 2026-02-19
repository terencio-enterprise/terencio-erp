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

    Long save(UUID organizationId, String username, String email, String fullName, String pinHash, String passwordHash);

    void update(Long id, String fullName, String email, boolean isActive);

    void updatePin(Long id, String newPinHash);

    void updatePassword(Long id, String newPasswordHash);

    void syncAccessGrants(Long employeeId, String role, UUID companyId, UUID storeId);

    List<AccessGrant> findAccessGrants(Long employeeId);

    void updateLastActiveContext(Long id, UUID companyId, UUID storeId);
}