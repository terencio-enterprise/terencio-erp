package es.terencio.erp.organization.application.usecase;

import java.util.UUID;

/**
 * Result of company creation.
 */
public record CreateCompanyResult(
        UUID companyId,
        String name,
        String taxId) {
}
