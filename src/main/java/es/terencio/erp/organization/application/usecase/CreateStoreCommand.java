package es.terencio.erp.organization.application.usecase;

import java.util.UUID;

/**
 * Command to create a new store.
 */
public record CreateStoreCommand(
        UUID companyId,
        String code,
        String name,
        String street,
        String zipCode,
        String city,
        String taxId,
        String timezone) {
}
