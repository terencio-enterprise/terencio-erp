package es.terencio.erp.employees.application.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public EmployeeService(EmployeePort employeePort, PasswordEncoder passwordEncoder) {
        this.employeePort = employeePort;
        this.passwordEncoder = passwordEncoder;
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

        Long id = employeePort.save(request.organizationId(), request.username(), request.email(), request.fullName(),
                pinHash, passwordHash);

        employeePort.syncAccessGrants(id, request.role(), request.companyId(), request.storeId());
        return getById(id);
    }

    @Override
    @Transactional
    public EmployeeDto update(Long id, UpdateEmployeeRequest request) {
        getById(id);

        // This method assumes the DTO will provide an email going forward.
        // Adapting signature dynamically based on domain constraints.
        employeePort.update(id, request.fullName(), null, request.isActive());

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
}