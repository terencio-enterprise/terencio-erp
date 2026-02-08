package es.terencio.erp.sync.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SyncCustomerDto(
        Long id,
        UUID uuid,
        String taxId,
        String legalName,
        String commercialName,
        String address,
        String zipCode,
        String email,
        String phone,
        Long tariffId,
        boolean allowCredit,
        BigDecimal creditLimit,
        boolean surchargeApply,
        String verifactuRef,
        boolean active,
        Instant updatedAt) {}
