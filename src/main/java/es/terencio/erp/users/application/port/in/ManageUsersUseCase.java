package es.terencio.erp.users.application.port.in;

import java.util.List;

import es.terencio.erp.users.application.dto.CreateUserRequest;
import es.terencio.erp.users.application.dto.UpdateUserRequest;
import es.terencio.erp.users.application.dto.UserDto;

public interface ManageUsersUseCase {
    List<UserDto> listAll();
    UserDto getById(Long id);
    UserDto create(CreateUserRequest request);
    UserDto update(Long id, UpdateUserRequest request);
    void changePosPin(Long id, String newPin);
    void changeBackofficePassword(Long id, String newPassword);
}