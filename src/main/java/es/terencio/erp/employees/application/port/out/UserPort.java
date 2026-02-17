package es.terencio.erp.employees.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import es.terencio.erp.employees.application.dto.UserDto;
import es.terencio.erp.employees.application.dto.UserSyncDto;

public interface UserPort {
    List<UserDto> findAll();

    Optional<UserDto> findById(Long id);

    Optional<UserDto> findByUsername(String username);

    List<UserDto> findByStoreId(UUID storeId);

    List<UserSyncDto> findSyncDataByStoreId(UUID storeId);

    Long save(String username, String fullName, String role, String pinHash, String passwordHash, UUID companyId,
            UUID storeId, String permissionsJson);

    void update(Long id, String fullName, String role, UUID storeId, boolean isActive, String permissionsJson);

    void syncAccessGrants(Long userId, String role, UUID companyId, UUID storeId);

    void updatePin(Long id, String newPinHash);

    void updatePassword(Long id, String newPasswordHash);
}