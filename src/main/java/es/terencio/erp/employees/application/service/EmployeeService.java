package es.terencio.erp.employees.application.service;

import java.util.Collections;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.terencio.erp.employees.application.dto.CreateEmployeeRequest;
import es.terencio.erp.employees.application.dto.EmployeeDto;
import es.terencio.erp.employees.application.dto.UpdateEmployeeRequest;
import es.terencio.erp.employees.application.port.in.ManageEmployeesUseCase;
import es.terencio.erp.employees.application.port.out.EmployeePort;
import es.terencio.erp.shared.exception.DomainException;

@Service
public class EmployeeService implements ManageEmployeesUseCase {

    private final EmployeePort employeePort;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public EmployeeService(EmployeePort employeePort, PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.employeePort = employeePort;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<EmployeeDto> listAll() {
        return employeePort.findAll();
    }

    @Override
    public EmployeeDto getById(Long id) {
        return employeePort.findById(id).orElseThrow(() -> new DomainException("Employee not found"));
    }

    @Override
    @Transactional
    public EmployeeDto create(CreateEmployeeRequest request) {
        if (employeePort.findByUsername(request.username()).isPresent()) {
            throw new DomainException("Username already exists");
        }
        String pinHash = passwordEncoder.encode(request.posPin());
        String passwordHash = passwordEncoder.encode(request.backofficePassword());
        String permissionsJson = toJson(
                request.permissions() != null ? request.permissions() : Collections.emptyList());

        Long id = employeePort.save(request.username(), request.fullName(), request.role(), pinHash, passwordHash,
                request.companyId(), request.storeId(), permissionsJson);
        employeePort.syncAccessGrants(id, request.role(), request.companyId(), request.storeId());
        return getById(id);
    }

    @Override
    @Transactional
    public EmployeeDto update(Long id, UpdateEmployeeRequest request) {
        getById(id);
        String permissionsJson = toJson(
                request.permissions() != null ? request.permissions() : Collections.emptyList());
        employeePort.update(id, request.fullName(), request.role(), request.storeId(), request.isActive(),
                permissionsJson);
        employeePort.syncAccessGrants(id, request.role(), null, request.storeId());
        return getById(id);
    }

    @Override
    @Transactional
    public void changePosPin(Long id, String newPin) {
        getById(id);
        employeePort.updatePin(id, passwordEncoder.encode(newPin));
    }

    @Override
    @Transactional
    public void changeBackofficePassword(Long id, String newPassword) {
        getById(id);
        employeePort.updatePassword(id, passwordEncoder.encode(newPassword));
    }

    private String toJson(List<String> permissions) {
        try {
            return objectMapper.writeValueAsString(permissions);
        } catch (JsonProcessingException e) {
            throw new DomainException("Error serializing permissions");
        }
    }
}
