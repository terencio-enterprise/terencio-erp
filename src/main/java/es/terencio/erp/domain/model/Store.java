package es.terencio.erp.domain.model;

import java.util.UUID;

public record Store(
    UUID id,
    String code,
    String name,
    String address,
    String taxId,
    boolean isActive
) {}