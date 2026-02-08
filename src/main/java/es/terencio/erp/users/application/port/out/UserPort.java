package es.terencio.erp.users.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import es.terencio.erp.users.application.dto.UserDto;

public interface UserPort {
    List<UserDto> findAll();
    Optional<UserDto> findById(Long id);
    Optional<UserDto> findByUsername(String username);
    Long save(String username, String fullName, String role, String pinHash, String passwordHash, UUID storeId, String permissionsJson);
    void update(Long id, String fullName, String role, UUID storeId, boolean isActive, String permissionsJson);
    void updatePin(Long id, String newPinHash);
    void updatePassword(Long id, String newPasswordHash);
}