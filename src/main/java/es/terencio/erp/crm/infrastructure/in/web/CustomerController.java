package es.terencio.erp.crm.infrastructure.in.web;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.auth.domain.model.Permission;
import es.terencio.erp.auth.infrastructure.config.security.aop.RequiresPermission;
import es.terencio.erp.crm.application.port.in.ManageCustomerUseCase;
import es.terencio.erp.crm.application.port.in.ManageCustomerUseCase.CreateCustomerCommand;
import es.terencio.erp.crm.application.port.in.ManageCustomerUseCase.CustomerQuery;
import es.terencio.erp.crm.application.port.in.ManageCustomerUseCase.UpdateCustomerCommand;
import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.crm.domain.model.CustomerType;
import es.terencio.erp.shared.domain.query.PageResult;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/customers")
@RequiredArgsConstructor
@Tag(name = "Customers CRUD", description = "CRM Customer Management & Filtering")
public class CustomerController {

    private final ManageCustomerUseCase manageCustomerUseCase;

    @GetMapping
    @Operation(summary = "Search customers with paginated filters")
    @RequiresPermission(permission = Permission.CUSTOMER_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<PageResult<CustomerResponse>>> searchCustomers(
            @PathVariable UUID companyId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CustomerType type,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        CustomerQuery query = new CustomerQuery(search, type, active, page, size);
        PageResult<Customer> results = manageCustomerUseCase.search(companyId, query);
        
        PageResult<CustomerResponse> response = results.map(CustomerResponse::fromDomain);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{customerUuid}")
    @Operation(summary = "Get customer details")
    @RequiresPermission(permission = Permission.CUSTOMER_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(
            @PathVariable UUID companyId, @PathVariable UUID customerUuid) {
        
        Customer customer = manageCustomerUseCase.getByUuid(companyId, customerUuid);
        return ResponseEntity.ok(ApiResponse.success(CustomerResponse.fromDomain(customer)));
    }

    @PostMapping
    @Operation(summary = "Create a new customer/client")
    @RequiresPermission(permission = Permission.CUSTOMER_CREATE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @PathVariable UUID companyId, @Valid @RequestBody CreateCustomerCommand command) {
        
        Customer saved = manageCustomerUseCase.create(companyId, command);
        return ResponseEntity.ok(ApiResponse.success("Customer created", CustomerResponse.fromDomain(saved)));
    }

    @PutMapping("/{customerUuid}")
    @Operation(summary = "Update an existing customer")
    @RequiresPermission(permission = Permission.CUSTOMER_UPDATE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable UUID companyId, 
            @PathVariable UUID customerUuid, 
            @Valid @RequestBody UpdateCustomerCommand command) {
        
        Customer updated = manageCustomerUseCase.update(companyId, customerUuid, command);
        return ResponseEntity.ok(ApiResponse.success("Customer updated", CustomerResponse.fromDomain(updated)));
    }

    @DeleteMapping("/{customerUuid}")
    @Operation(summary = "Soft delete a customer")
    @RequiresPermission(permission = Permission.CUSTOMER_DELETE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(
            @PathVariable UUID companyId, @PathVariable UUID customerUuid) {
        
        manageCustomerUseCase.delete(companyId, customerUuid);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted", null));
    }
}