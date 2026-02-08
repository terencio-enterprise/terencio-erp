package es.terencio.erp.stores.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record StoreDto(
    UUID id,
    
    @NotBlank(message = "Store Code is required (e.g. MAD-01)")
    String code,
    
    @NotBlank(message = "Store Name is required")
    String name,
    
    String address,
    String taxId,
    boolean isActive
) {}