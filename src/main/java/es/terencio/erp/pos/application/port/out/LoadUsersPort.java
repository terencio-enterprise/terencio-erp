package es.terencio.erp.pos.application.port.out;

import java.util.List;
import java.util.UUID;

import es.terencio.erp.users.application.dto.UserDto;

/**
 * Output port for loading user information.
 * Application layer depends on this interface.
 * Infrastructure layer provides the implementation.
 */
public interface LoadUsersPort {

    /**
     * Load all users for a specific store.
     * 
     * @param storeId the store UUID
     * @return list of users
     */
    List<UserDto> loadByStore(UUID storeId);
}
