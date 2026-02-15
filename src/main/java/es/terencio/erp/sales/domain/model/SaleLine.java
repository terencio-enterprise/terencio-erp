package es.terencio.erp.sales.domain.model;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.identifier.SaleId;
import es.terencio.erp.shared.domain.valueobject.Money;
import es.terencio.erp.shared.domain.valueobject.Percentage;
import es.terencio.erp.shared.domain.valueobject.Quantity;
import es.terencio.erp.shared.domain.valueobject.TaxRate;

/**
 * SaleLine entity.
 * Represents a line item in a sale.
 */
public class SaleLine {

    private final Long id;
    private final SaleId saleUuid;
    private final ProductId productId;
    private final String description;
    private final Quantity quantity;
    private final Money unitPrice;
    private final Percentage discountPercent;
    private final Money discountAmount;
    private final Long taxId;
    private final TaxRate taxRate;
    private final Money taxAmount;
    private final Money totalLine;
    private final String pricingContext;

    public SaleLine(
            Long id,
            SaleId saleUuid,
            ProductId productId,
            String description,
            Quantity quantity,
            Money unitPrice,
            Percentage discountPercent,
            Money discountAmount,
            Long taxId,
            TaxRate taxRate,
            Money taxAmount,
            Money totalLine,
            String pricingContext) {

        if (saleUuid == null)
            throw new InvariantViolationException("Sale UUID cannot be null");
        if (description == null || description.isBlank())
            throw new InvariantViolationException("Description cannot be empty");
        if (quantity == null || !quantity.isPositive())
            throw new InvariantViolationException("Quantity must be positive");
        if (unitPrice == null)
            throw new InvariantViolationException("Unit price cannot be null");
        if (taxRate == null)
            throw new InvariantViolationException("Tax rate cannot be null");

        this.id = id;
        this.saleUuid = saleUuid;
        this.productId = productId;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discountPercent = discountPercent != null ? discountPercent : Percentage.zero();
        this.discountAmount = discountAmount != null ? discountAmount : Money.zeroEuros();
        this.taxId = taxId;
        this.taxRate = taxRate;
        this.taxAmount = taxAmount != null ? taxAmount : Money.zeroEuros();
        this.totalLine = totalLine != null ? totalLine : Money.zeroEuros();
        this.pricingContext = pricingContext;
    }

    public static SaleLine create(
            SaleId saleUuid,
            ProductId productId,
            String description,
            Quantity quantity,
            Money unitPrice,
            Long taxId,
            TaxRate taxRate) {

        Money subtotal = unitPrice.multiply(quantity.value());
        Money taxAmount = taxRate.calculateTaxAmount(subtotal);
        Money totalLine = subtotal.add(taxAmount);

        return new SaleLine(
                null,
                saleUuid,
                productId,
                description,
                quantity,
                unitPrice,
                Percentage.zero(),
                Money.zeroEuros(),
                taxId,
                taxRate,
                taxAmount,
                totalLine,
                null);
    }

    public Money netAmount() {
        return unitPrice.multiply(quantity.value()).subtract(discountAmount);
    }

    // Getters
    public Long id() {
        return id;
    }

    public SaleId saleUuid() {
        return saleUuid;
    }

    public ProductId productId() {
        return productId;
    }

    public String description() {
        return description;
    }

    public Quantity quantity() {
        return quantity;
    }

    public Money unitPrice() {
        return unitPrice;
    }

    public Percentage discountPercent() {
        return discountPercent;
    }

    public Money discountAmount() {
        return discountAmount;
    }

    public Long taxId() {
        return taxId;
    }

    public TaxRate taxRate() {
        return taxRate;
    }

    public Money taxAmount() {
        return taxAmount;
    }

    public Money totalLine() {
        return totalLine;
    }

    public String pricingContext() {
        return pricingContext;
    }
}
