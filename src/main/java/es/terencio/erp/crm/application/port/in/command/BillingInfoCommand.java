package es.terencio.erp.crm.application.port.in.command;

public record BillingInfoCommand(
        Long tariffId,
        Boolean allowCredit,
        Long creditLimitCents,
        Boolean surchargeApply
) {}