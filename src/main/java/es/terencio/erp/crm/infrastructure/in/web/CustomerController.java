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
import es.terencio.erp.crm.application.port.out.CustomerProductPriceRepository;
import es.terencio.erp.crm.application.port.out.CustomerRepository;
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

    private final CustomerRepository customerRepository;
    private final CustomerProductPriceRepository priceRepository;

    public CustomerController(CustomerRepository customerRepository, CustomerProductPriceRepository priceRepository) {
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

        List<CustomerResponse> response = customers.stream()
                .map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success("Customers fetched successfully", response));
    }

    @PostMapping
    @Operation(summary = "Create customer")
    @RequiresPermission(permission = Permission.CUSTOMER_CREATE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = Customer.create(new CompanyId(request.companyId()), request.legalName(),
                request.taxId() != null ? TaxId.of(request.taxId()) : null);
        if (request.email() != null)
            customer.updateContactInfo(Email.of(request.email()), request.phone(), request.address(), request.zipCode(),
                    request.city());
        Customer saved = customerRepository.save(customer);
        return ResponseEntity.ok(ApiResponse.success("Customer created", toResponse(saved)));
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "Update customer contact info")
    @RequiresPermission(permission = Permission.CUSTOMER_UPDATE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable UUID uuid, @RequestParam UUID companyId, @RequestBody UpdateCustomerRequest request) {
        Customer customer = customerRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + uuid));
        if (request.email() != null)
            customer.updateContactInfo(Email.of(request.email()), request.phone(), request.address(), request.zipCode(),
                    request.city());
        Customer saved = customerRepository.save(customer);
        return ResponseEntity.ok(ApiResponse.success("Customer updated", toResponse(saved)));
    }

    @PutMapping("/{uuid}/commercial-terms")
    @Operation(summary = "Update customer commercial terms")
    @RequiresPermission(permission = Permission.CUSTOMER_UPDATE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<Void> updateCommercialTerms(
            @PathVariable UUID uuid, @RequestParam UUID companyId, @RequestBody CommercialTermsRequest request) {
        Customer customer = customerRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + uuid));
        customer.assignTariff(request.tariffId());
        customer.configureCreditSettings(request.allowCredit(), Money.ofEurosCents(request.creditLimitCents()));
        customerRepository.save(customer);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{uuid}/special-prices")
    @Operation(summary = "Get customer special prices")
    @RequiresPermission(permission = Permission.CUSTOMER_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<List<SpecialPriceResponse>>> getSpecialPrices(
            @PathVariable UUID uuid, @RequestParam UUID companyId) {
        Customer customer = customerRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + uuid));
        List<CustomerProductPrice> prices = priceRepository.findByCustomerId(customer.id());
        List<SpecialPriceResponse> response = prices.stream()
                .map(p -> new SpecialPriceResponse(p.productId().value(), p.customPrice().cents()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Special prices fetched", response));
    }

    @PutMapping("/{uuid}/special-prices")
    @Operation(summary = "Update customer special prices")
    @RequiresPermission(permission = Permission.CUSTOMER_UPDATE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<Void> updateSpecialPrices(
            @PathVariable UUID uuid, @RequestParam UUID companyId, @RequestBody SpecialPricesRequest request) {
        Customer customer = customerRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + uuid));
        for (SpecialPriceEntry entry : request.prices()) {
            CustomerProductPrice price = CustomerProductPrice.create(customer.id(), new ProductId(entry.productId()),
                    Money.ofEurosCents(entry.priceCents()));
            priceRepository.save(price);
        }
        return ResponseEntity.ok().build();
    }

    private CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(c.uuid(), c.legalName(), c.taxId() != null ? c.taxId().value() : null,
                c.email() != null ? c.email().value() : null, c.phone(), c.tariffId(), c.allowCredit(),
                c.creditLimit().cents());
    }

    public record CustomerResponse(UUID uuid, String legalName, String taxId, String email, String phone, Long tariffId,
            boolean allowCredit, long creditLimitCents) {
    }

    public record CreateCustomerRequest(UUID companyId, String legalName, String taxId, String email, String phone,
            String address, String zipCode, String city) {
    }

    public record UpdateCustomerRequest(String email, String phone, String address, String zipCode, String city) {
    }

    public record CommercialTermsRequest(Long tariffId, boolean allowCredit, long creditLimitCents) {
    }

    public record SpecialPricesRequest(List<SpecialPriceEntry> prices) {
    }

    public record SpecialPriceEntry(Long productId, long priceCents) {
    }

    public record SpecialPriceResponse(Long productId, long priceCents) {
    }
}
