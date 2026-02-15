package es.terencio.erp.catalog.application.usecase;

import es.terencio.erp.catalog.application.port.in.ResolveEffectivePriceUseCase;
import es.terencio.erp.catalog.application.port.out.ProductPriceRepository;
import es.terencio.erp.catalog.application.port.out.ProductRepository;
import es.terencio.erp.catalog.domain.model.PricingResult;
import es.terencio.erp.catalog.domain.model.ProductPrice;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.exception.DomainException;

/**
 * Service for resolving effective prices.
 * Priority: Customer custom price > Promotion > Tariff.
 * (Simplified: does not yet implement full promotion logic)
 */
public class ResolveEffectivePriceService implements ResolveEffectivePriceUseCase {

    private final ProductRepository productRepository;
    private final ProductPriceRepository productPriceRepository;

    public ResolveEffectivePriceService(
            ProductRepository productRepository,
            ProductPriceRepository productPriceRepository) {
        this.productRepository = productRepository;
        this.productPriceRepository = productPriceRepository;
    }

    @Override
    public PricingResult execute(ResolveEffectivePriceCommand command) {
        ProductId productId = new ProductId(command.productId());

        // Validate product exists
        productRepository.findById(productId)
                .orElseThrow(() -> new DomainException("Product not found"));

        // TODO: Check customer custom prices (from CRM module port)
        // TODO: Check active promotions/pricing rules

        // Fallback to tariff price
        Long tariffId = command.tariffIdOverride() != null
                ? command.tariffIdOverride()
                : 1L; // Default tariff

        ProductPrice price = productPriceRepository.findByProductAndTariff(productId, tariffId)
                .orElseThrow(() -> new DomainException("No price found for product in tariff"));

        return PricingResult.fromTariff(price.price(), tariffId);
    }
}
