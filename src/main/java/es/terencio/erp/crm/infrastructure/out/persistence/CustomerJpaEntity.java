package es.terencio.erp.crm.infrastructure.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    // Business Data
    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "commercial_name")
    private String commercialName;

    private String email;
    @Column(length = 50)
    private String phone;
    private String address;
    @Column(name = "zip_code", length = 20)
    private String zipCode;
    @Column(length = 100)
    private String city;
    @Column(length = 10)
    private String country;

    // Commercial & Billing Data
    @Column(name = "tariff_id")
    private Long tariffId;
    @Column(name = "allow_credit")
    private boolean allowCredit;
    @Column(name = "credit_limit")
    private Long creditLimit;
    @Column(name = "surcharge_apply")
    private boolean surchargeApply;

    // CRM & Marketing Data
    @Column(length = 50)
    private String type;
    @Column(length = 50)
    private String origin;
    
    @Column(columnDefinition = "text[]")
    private List<String> tags;

    @Column(name = "marketing_consent")
    private boolean marketingConsent;
    @Column(name = "marketing_status", length = 20)
    private String marketingStatus;
    @Column(name = "unsubscribe_token", length = 64, unique = true)
    private String unsubscribeToken;

    @Column(name = "last_interaction_at")
    private Instant lastInteractionAt;
    
    private String notes;
    private boolean active;

    // Audit
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;
    @Column(name = "deleted_at")
    private Instant deletedAt;
}