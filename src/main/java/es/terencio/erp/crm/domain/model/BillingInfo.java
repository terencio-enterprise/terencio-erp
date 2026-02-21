package es.terencio.erp.crm.domain.model;

public record BillingInfo(
        Long tariffId, 
        boolean allowCredit, 
        Long creditLimitCents, 
        boolean surchargeApply
) {
    public static BillingInfo defaultSettings() { 
        return new BillingInfo(null, false, 0L, false); 
    }
}