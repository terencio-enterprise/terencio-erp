package es.terencio.erp.sync.application.dto;

public record SyncTariffDto(
        Long id,
        String name,
        int priority,
        boolean active) {}
