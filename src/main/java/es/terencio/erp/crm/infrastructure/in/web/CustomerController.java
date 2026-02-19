package es.terencio.erp.crm.infrastructure.in.web;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.auth.domain.model.Permission;
import es.terencio.erp.auth.infrastructure.config.security.aop.RequiresPermission;
import es.terencio.erp.crm.application.port.out.*;
import es.terencio.erp.crm.domain.model.*;
import es.terencio.erp.shared.domain.identifier.*;
import es.terencio.erp.shared.domain.valueobject.*;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "CRM customer management")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping
    @Operation(summary = "Search customers")
    @RequiresPermission(permission = Permission.CUSTOMER_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> searchCustomers(
            @RequestParam UUID companyId, @RequestParam(required = false) String search) {
        List<Customer> customers = (search != null && !search.isBlank()) 
            ? customerRepository.searchCustomers(new CompanyId(companyId), search)
            : customerRepository.findByCompanyId(new CompanyId(companyId));
            
        List<CustomerResponse> response = customers.stream()
                .map(c -> new CustomerResponse(c.uuid(), c.legalName(), c.taxId() != null ? c.taxId().value() : null, c.email() != null ? c.email().value() : null, c.phone(), c.tariffId(), c.allowCredit(), c.creditLimit().cents())).toList();
        return ResponseEntity.ok(ApiResponse.success("Customers fetched successfully", response));
    }

    @PostMapping
    @Operation(summary = "Create customer")
    @RequiresPermission(permission = Permission.CUSTOMER_CREATE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = Customer.create(new CompanyId(request.companyId()), request.legalName(), request.taxId() != null ? TaxId.of(request.taxId()) : null);
        if (request.email() != null) customer.updateContactInfo(Email.of(request.email()), request.phone(), request.address(), request.zipCode(), request.city());
        Customer saved = customerRepository.save(customer);
        return ResponseEntity.ok(ApiResponse.success("Customer created", new CustomerResponse(saved.uuid(), saved.legalName(), saved.taxId() != null ? saved.taxId().value() : null, saved.email() != null ? saved.email().value() : null, saved.phone(), saved.tariffId(), saved.allowCredit(), saved.creditLimit().cents())));
    }

    public record CustomerResponse(UUID uuid, String legalName, String taxId, String email, String phone, Long tariffId, boolean allowCredit, long creditLimitCents) {}
    public record CreateCustomerRequest(UUID companyId, String legalName, String taxId, String email, String phone, String address, String zipCode, String city) {}
}
