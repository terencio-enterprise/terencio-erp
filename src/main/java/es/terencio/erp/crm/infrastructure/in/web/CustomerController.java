package es.terencio.erp.crm.infrastructure.in.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
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
import es.terencio.erp.crm.application.port.out.CustomerProductPriceRepositoryPort;
import es.terencio.erp.crm.application.port.out.CustomerRepositoryPort;
import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.crm.domain.model.CustomerProductPrice;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.valueobject.Email;
import es.terencio.erp.shared.domain.valueobject.Money;
import es.terencio.erp.shared.domain.valueobject.TaxId;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "CRM customer management")
public class CustomerController {

        private final CustomerRepositoryPort customerRepository;

        public CustomerController(CustomerRepositoryPort customerRepository,
                        CustomerProductPriceRepositoryPort priceRepository) {
                this.customerRepository = customerRepository;
                this.priceRepository = priceRepository;
        }

        @GetMapping
        @Operation(summary = "Search customers")
        @RequiresPermission(permission = Permission.CUSTOMER_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
        public ResponseEntity<ApiResponse<List<CustomerResponse>>> searchCustomers(
                        @RequestParam UUID companyId, @RequestParam(required = false) String search) {

                List<Customer> customers = (search != null && !search.isBlank())
                                ? customerRepository.searchCustomers(new CompanyId(companyId), search)
                                : customerRepository.findByCompanyId(new CompanyId(companyId));

                return ResponseEntity.ok(ApiResponse.success(customers.stream().map(this::toResponse).toList()));
        }

        @PostMapping
        @Operation(summary = "Create customer")
        @RequiresPermission(permission = Permission.CUSTOMER_CREATE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
        public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
                        @Valid @RequestBody CreateCustomerRequest request) {
                Customer customer = Customer.create(
                                new CompanyId(request.companyId()),
                                request.legalName(),
                                request.taxId() != null ? TaxId.of(request.taxId()) : null);

                if (request.email() != null) {
                        customer.updateContactInfo(Email.of(request.email()), request.phone(), request.address(),
                                        request.zipCode(), request.city());
                }

                Customer saved = customerRepository.save(customer);
                return ResponseEntity.ok(ApiResponse.success("Customer created", toResponse(saved)));
        }
}