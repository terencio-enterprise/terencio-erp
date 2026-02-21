package es.terencio.erp.crm.infrastructure.in.web;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.auth.domain.model.Permission;
import es.terencio.erp.auth.infrastructure.config.security.aop.RequiresPermission;
import es.terencio.erp.crm.application.port.in.ManageCustomerUseCase;
import es.terencio.erp.crm.application.port.in.query.SearchCustomerQuery;
import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.crm.domain.model.CustomerType;
import es.terencio.erp.crm.infrastructure.in.web.dto.CreateCustomerRequest;
import es.terencio.erp.crm.infrastructure.in.web.dto.CustomerResponse;
import es.terencio.erp.crm.infrastructure.in.web.dto.UpdateCustomerRequest;
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

        SearchCustomerQuery query = new SearchCustomerQuery(search, type, active, page, size);
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
            @PathVariable UUID companyId,
            @Valid @RequestBody CreateCustomerRequest request) {

        Customer saved = manageCustomerUseCase.create(companyId, request.toCommand());

        return ResponseEntity.status(201)
                .body(ApiResponse.success("Customer created", CustomerResponse.fromDomain(saved)));
    }

    @PutMapping("/{customerUuid}")
    @Operation(summary = "Update an existing customer")
    @RequiresPermission(permission = Permission.CUSTOMER_UPDATE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable UUID companyId,
            @PathVariable UUID customerUuid,
            @Valid @RequestBody UpdateCustomerRequest request) {

        Customer updated = manageCustomerUseCase.update(companyId, customerUuid, request.toCommand());
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