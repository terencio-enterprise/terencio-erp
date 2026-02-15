package es.terencio.erp.crm.presentation;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
import jakarta.validation.Valid;

/**
 * REST controller for CRM (Customer management).
 */
@RestController
@RequestMapping("/api/v1/customers")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final CustomerProductPriceRepository customerProductPriceRepository;

    public CustomerController(
            CustomerRepository customerRepository,
            CustomerProductPriceRepository customerProductPriceRepository) {
        this.customerRepository = customerRepository;
        this.customerProductPriceRepository = customerProductPriceRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> searchCustomers(
            @RequestParam UUID companyId,
            @RequestParam(required = false) String search) {

        List<Customer> customers;

        if (search != null && !search.isBlank()) {
            customers = customerRepository.searchCustomers(new CompanyId(companyId), search);
        } else {
            customers = customerRepository.findByCompanyId(new CompanyId(companyId));
        }

        List<CustomerResponse> response = customers.stream()
                .map(c -> new CustomerResponse(
                        c.uuid(),
                        c.legalName(),
                        c.taxId() != null ? c.taxId().value() : null,
                        c.email() != null ? c.email().value() : null,
                        c.phone(),
                        c.tariffId(),
                        c.allowCredit(),
                        c.creditLimit().cents()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Customers fetched successfully", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        CompanyId companyId = new CompanyId(request.companyId());
        TaxId taxId = request.taxId() != null ? TaxId.of(request.taxId()) : null;

        Customer customer = Customer.create(companyId, request.legalName(), taxId);

        if (request.email() != null) {
            customer.updateContactInfo(
                    Email.of(request.email()),
                    request.phone(),
                    request.address(),
                    request.zipCode(),
                    request.city());
        }

        Customer saved = customerRepository.save(customer);

        return ResponseEntity.ok(ApiResponse.success("Customer created successfully", new CustomerResponse(
                saved.uuid(),
                saved.legalName(),
                saved.taxId() != null ? saved.taxId().value() : null,
                saved.email() != null ? saved.email().value() : null,
                saved.phone(),
                saved.tariffId(),
                saved.allowCredit(),
                saved.creditLimit().cents())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request) {

        Customer customer = customerRepository.findByUuid(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (request.email() != null || request.phone() != null) {
            customer.updateContactInfo(
                    request.email() != null ? Email.of(request.email()) : customer.email(),
                    request.phone() != null ? request.phone() : customer.phone(),
                    request.address(),
                    request.zipCode(),
                    request.city());
        }

        Customer saved = customerRepository.save(customer);

        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", new CustomerResponse(
                saved.uuid(),
                saved.legalName(),
                saved.taxId() != null ? saved.taxId().value() : null,
                saved.email() != null ? saved.email().value() : null,
                saved.phone(),
                saved.tariffId(),
                saved.allowCredit(),
                saved.creditLimit().cents())));
    }

    @PutMapping("/{id}/commercial-terms")
    public ResponseEntity<ApiResponse<Void>> updateCommercialTerms(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCommercialTermsRequest request) {

        Customer customer = customerRepository.findByUuid(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (request.tariffId() != null) {
            customer.assignTariff(request.tariffId());
        }

        if (request.allowCredit() != null) {
            customer.configureCreditSettings(
                    request.allowCredit(),
                    Money.ofEurosCents(request.creditLimitCents() != null ? request.creditLimitCents() : 0));
        }

        customerRepository.save(customer);

        return ResponseEntity.ok(ApiResponse.success("Commercial terms updated successfully"));
    }

    @GetMapping("/{id}/special-prices")
    public ResponseEntity<ApiResponse<List<SpecialPriceResponse>>> getSpecialPrices(@PathVariable UUID id) {
        Customer customer = customerRepository.findByUuid(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        List<CustomerProductPrice> prices = customerProductPriceRepository.findByCustomerId(customer.id());

        List<SpecialPriceResponse> response = prices.stream()
                .map(p -> new SpecialPriceResponse(p.productId().value(), p.customPrice().cents()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Special prices fetched successfully", response));
    }

    @PutMapping("/{id}/special-prices")
    public ResponseEntity<ApiResponse<Void>> updateSpecialPrices(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSpecialPricesRequest request) {

        Customer customer = customerRepository.findByUuid(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        for (UpdateSpecialPricesRequest.ProductPrice pp : request.prices()) {
            CustomerProductPrice price = CustomerProductPrice.create(
                    customer.id(),
                    new ProductId(pp.productId()),
                    Money.ofEurosCents(pp.priceCents()));
            customerProductPriceRepository.save(price);
        }

        return ResponseEntity.ok(ApiResponse.success("Special prices updated successfully"));
    }

    // ==================== RECORDS ====================

    public record CustomerResponse(UUID uuid, String legalName, String taxId, String email,
            String phone, Long tariffId, boolean allowCredit, long creditLimitCents) {
    }

    public record CreateCustomerRequest(UUID companyId, String legalName, String taxId, String email,
            String phone, String address, String zipCode, String city) {
    }

    public record UpdateCustomerRequest(String email, String phone, String address, String zipCode, String city) {
    }

    public record UpdateCommercialTermsRequest(Long tariffId, Boolean allowCredit, Long creditLimitCents) {
    }

    public record SpecialPriceResponse(Long productId, long priceCents) {
    }

    public record UpdateSpecialPricesRequest(List<ProductPrice> prices) {
        public record ProductPrice(Long productId, long priceCents) {
        }
    }
}
