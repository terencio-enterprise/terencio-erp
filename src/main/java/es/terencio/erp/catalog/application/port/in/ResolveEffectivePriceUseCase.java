package es.terencio.erp.catalog.application.port.in;

import es.terencio.erp.catalog.application.usecase.ResolveEffectivePriceCommand;
import es.terencio.erp.catalog.domain.model.PricingResult;

/**
 * Use case for resolving the effective price for a product.
 * Considers tariffs, customer-specific prices, and promotions.
 */
public interface ResolveEffectivePriceUseCase {
    PricingResult execute(ResolveEffectivePriceCommand command);
}
