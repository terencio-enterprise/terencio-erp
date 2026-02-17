package es.terencio.erp.employees.application.service;

import java.util.Collections;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.terencio.erp.shared.exception.DomainException;
import es.terencio.erp.employees.application.dto.CreateUserRequest;
import es.terencio.erp.employees.application.dto.UpdateUserRequest;
import es.terencio.erp.employees.application.dto.UserDto;
import es.terencio.erp.employees.application.port.in.ManageUsersUseCase;
import es.terencio.erp.employees.application.port.out.UserPort;

@Service
public class UserService implements ManageUsersUseCase {

    private final UserPort userPort;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public UserService(UserPort userPort, PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.userPort = userPort;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<UserDto> listAll() {
        return userPort.findAll();
    }

    @Override
    public UserDto getById(Long id) {
        return userPort.findById(id).orElseThrow(() -> new DomainException("User not found"));
    }

    @Override
    @Transactional
    public UserDto create(CreateUserRequest request) {
        if (userPort.findByUsername(request.username()).isPresent()) {
            throw new DomainException("Username already exists");
        }
        String pinHash = passwordEncoder.encode(request.posPin());
        String passwordHash = passwordEncoder.encode(request.backofficePassword());
        String permissionsJson = toJson(
                request.permissions() != null ? request.permissions() : Collections.emptyList());

        Long id = userPort.save(request.username(), request.fullName(), request.role(), pinHash, passwordHash,
                request.companyId(), request.storeId(), permissionsJson);
        userPort.syncAccessGrants(id, request.role(), request.companyId(), request.storeId());
        return getById(id);
    }

    @Override
    @Transactional
    public UserDto update(Long id, UpdateUserRequest request) {
        getById(id);
        String permissionsJson = toJson(
                request.permissions() != null ? request.permissions() : Collections.emptyList());
        userPort.update(id, request.fullName(), request.role(), request.storeId(), request.isActive(), permissionsJson);
        userPort.syncAccessGrants(id, request.role(), null, request.storeId());
        return getById(id);
    }

    @Override
    @Transactional
    public void changePosPin(Long id, String newPin) {
        getById(id);
        userPort.updatePin(id, passwordEncoder.encode(newPin));
    }

    @Override
    @Transactional
    public void changeBackofficePassword(Long id, String newPassword) {
        getById(id);
        userPort.updatePassword(id, passwordEncoder.encode(newPassword));
    }

    private String toJson(List<String> permissions) {
        try {
            return objectMapper.writeValueAsString(permissions);
        } catch (JsonProcessingException e) {
            throw new DomainException("Error serializing permissions");
        }
    }
}