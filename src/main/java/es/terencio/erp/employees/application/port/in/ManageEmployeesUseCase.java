package es.terencio.erp.employees.application.port.in;

import java.util.List;

import es.terencio.erp.employees.application.dto.CreateEmployeeRequest;
import es.terencio.erp.employees.application.dto.UpdateEmployeeRequest;
import es.terencio.erp.employees.application.dto.EmployeeDto;

public interface ManageEmployeesUseCase {
    List<EmployeeDto> listAll();
    EmployeeDto getById(Long id);
    EmployeeDto create(CreateEmployeeRequest request);
    EmployeeDto update(Long id, UpdateEmployeeRequest request);
    void changePosPin(Long id, String newPin);
    void changeBackofficePassword(Long id, String newPassword);
}
