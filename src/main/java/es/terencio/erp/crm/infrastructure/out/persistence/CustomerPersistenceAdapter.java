package es.terencio.erp.crm.infrastructure.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import es.terencio.erp.crm.application.port.in.query.SearchCustomerQuery;
import es.terencio.erp.crm.application.port.out.CustomerRepositoryPort;
import es.terencio.erp.crm.domain.model.BillingInfo;
import es.terencio.erp.crm.domain.model.ContactInfo;
import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.crm.domain.model.CustomerType;
import es.terencio.erp.crm.domain.model.MarketingProfile;
import es.terencio.erp.crm.domain.model.MarketingStatus;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.CustomerId;
import es.terencio.erp.shared.domain.query.PageResult;
import es.terencio.erp.shared.domain.valueobject.Email;
import es.terencio.erp.shared.domain.valueobject.TaxId;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomerPersistenceAdapter implements CustomerRepositoryPort {

    private final SpringDataCustomerRepository repository;

    @Override
    public Customer save(Customer customer) {
        CustomerJpaEntity entity = toEntity(customer);
        CustomerJpaEntity savedEntity = repository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<Customer> findByUuidAndCompanyId(UUID uuid, CompanyId companyId) {
        return repository.findByUuidAndCompanyIdAndDeletedAtIsNull(uuid, companyId.value())
                .map(this::toDomain);
    }

    @Override
    public boolean existsByEmailAndCompanyId(Email email, CompanyId companyId) {
        if (email == null) return false;
        return repository.existsByEmailIgnoreCaseAndCompanyIdAndDeletedAtIsNull(email.value(), companyId.value());
    }

    @Override
    public PageResult<Customer> searchPaginated(CompanyId companyId, SearchCustomerQuery query) {
        Specification<CustomerJpaEntity> spec = buildSpecification(companyId.value(), query);
        Page<CustomerJpaEntity> page = repository.findAll(spec, PageRequest.of(query.page(), query.size()));

        return new PageResult<>(
                page.getContent().stream().map(this::toDomain).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }

    // --- Private Specification Builder ---

    private Specification<CustomerJpaEntity> buildSpecification(UUID companyId, SearchCustomerQuery query) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("companyId"), companyId));
            predicates.add(cb.isNull(root.get("deletedAt"))); // global filter

            if (query.search() != null && !query.search().isBlank()) {
                String searchPattern = "%" + query.search().toLowerCase() + "%";
                Predicate searchPred = cb.or(
                        cb.like(cb.lower(root.get("legalName")), searchPattern),
                        cb.like(cb.lower(root.get("commercialName")), searchPattern),
                        cb.like(cb.lower(root.get("email")), searchPattern),
                        cb.like(cb.lower(root.get("taxId")), searchPattern)
                );
                predicates.add(searchPred);
            }

            if (query.type() != null) {
                // assuming entity field "type" is stored as String enum name
                predicates.add(cb.equal(root.get("type"), query.type().name()));
            }

            if (query.active() != null) {
                predicates.add(cb.equal(root.get("active"), query.active()));
            }

            cq.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // --- Private Mappers (Hexagonal Output boundary translation) ---

    private CustomerJpaEntity toEntity(Customer domain) {
        if (domain == null) return null;

        ContactInfo ci = domain.getContactInfo() != null ? domain.getContactInfo() : ContactInfo.empty();
        BillingInfo bi = domain.getBillingInfo() != null ? domain.getBillingInfo() : BillingInfo.defaultSettings();
        MarketingProfile mp = domain.getMarketingProfile() != null ? domain.getMarketingProfile() : MarketingProfile.empty();

        return CustomerJpaEntity.builder()
                .id(domain.getId() != null ? domain.getId().value() : null)
                .uuid(domain.getUuid())
                .companyId(domain.getCompanyId().value())
                .taxId(domain.getTaxId() != null ? domain.getTaxId().value() : null)
                .legalName(domain.getLegalName())
                .commercialName(domain.getCommercialName())
                .type(domain.getType() != null ? domain.getType().name() : null)
                .active(domain.isActive())
                .notes(domain.getNotes())

                // Contact Info
                .email(ci.email() != null ? ci.email().value() : null)
                .phone(ci.phone())
                .address(ci.address())
                .zipCode(ci.zipCode())
                .city(ci.city())
                .country(ci.country())

                // Billing Info
                .tariffId(bi.tariffId())
                .allowCredit(bi.allowCredit())
                .creditLimit(bi.creditLimitCents())
                .surchargeApply(bi.surchargeApply())

                // Marketing Info
                .origin(mp.origin())
                .tags(mp.tags())
                .marketingConsent(mp.consent())
                .marketingStatus(mp.status() != null ? mp.status().name() : null)
                .unsubscribeToken(mp.unsubscribeToken())
                .lastInteractionAt(mp.lastInteractionAt())

                // Audit
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }

    private Customer toDomain(CustomerJpaEntity entity) {
        if (entity == null) return null;

        return Customer.builder()
                .id(entity.getId() != null ? new CustomerId(entity.getId()) : null)
                .uuid(entity.getUuid())
                .companyId(new CompanyId(entity.getCompanyId()))
                .taxId(entity.getTaxId() != null ? TaxId.of(entity.getTaxId()) : null)
                .legalName(entity.getLegalName())
                .commercialName(entity.getCommercialName())
                .type(entity.getType() != null ? CustomerType.valueOf(entity.getType()) : null)
                .active(entity.isActive())
                .notes(entity.getNotes())

                .contactInfo(new ContactInfo(
                        entity.getEmail() != null ? Email.of(entity.getEmail()) : null,
                        entity.getPhone(),
                        entity.getAddress(),
                        entity.getZipCode(),
                        entity.getCity(),
                        entity.getCountry() != null ? entity.getCountry() : "ES"
                ))

                .billingInfo(new BillingInfo(
                        entity.getTariffId(),
                        entity.isAllowCredit(),
                        entity.getCreditLimit(),
                        entity.isSurchargeApply()
                ))

                .marketingProfile(new MarketingProfile(
                        entity.getOrigin(),
                        entity.getTags() != null ? List.copyOf(entity.getTags()) : List.of(),
                        entity.isMarketingConsent(),
                        entity.getMarketingStatus() != null ? MarketingStatus.valueOf(entity.getMarketingStatus()) : null,
                        entity.getUnsubscribeToken(),
                        entity.getLastInteractionAt()
                ))

                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }
}