package es.terencio.erp.auth.domain.model;

public enum Permission {
    // ── Organization / Store management ────────────────────────────────────────
    ORGANIZATION_STORE_VIEW("organization:store:view"),
    ORGANIZATION_STORE_CREATE("organization:store:create"),
    ORGANIZATION_STORE_UPDATE("organization:store:update"),
    ORGANIZATION_STORE_DELETE("organization:store:delete"),

    ORGANIZATION_COMPANY_VIEW("organization:company:view"),
    ORGANIZATION_COMPANY_CREATE("organization:company:create"),
    ORGANIZATION_COMPANY_UPDATE("organization:company:update"),
    ORGANIZATION_COMPANY_DELETE("organization:company:delete"),

    // ── Products / Catalog ──────────────────────────────────────────────────────
    PRODUCT_VIEW("product:view"),
    PRODUCT_CREATE("product:create"),
    PRODUCT_UPDATE("product:update"),
    PRODUCT_DELETE("product:delete"),

    // ── Inventory ───────────────────────────────────────────────────────────────
    INVENTORY_VIEW("inventory:view"),
    INVENTORY_CREATE("inventory:create"),
    INVENTORY_UPDATE("inventory:update"),

    // ── Employees ───────────────────────────────────────────────────────────────
    EMPLOYEE_VIEW("employee:view"),
    EMPLOYEE_CREATE("employee:create"),
    EMPLOYEE_UPDATE("employee:update"),
    EMPLOYEE_DELETE("employee:delete"),

    // ── Customers / CRM ─────────────────────────────────────────────────────────
    CUSTOMER_VIEW("customer:view"),
    CUSTOMER_CREATE("customer:create"),
    CUSTOMER_UPDATE("customer:update"),
    CUSTOMER_DELETE("customer:delete"),

    // ── Sales ───────────────────────────────────────────────────────────────────
    SALE_VIEW("sale:view"),
    SALE_CREATE("sale:create"),
    SALE_REFUND("sale:refund"),
    SALE_VOID("sale:void"),

    // ── Devices ─────────────────────────────────────────────────────────────────
    DEVICE_VIEW("device:view"),
    DEVICE_MANAGE("device:manage"),

    // ── Reporting / Analytics ───────────────────────────────────────────────────
    REPORT_VIEW("report:view"),
    REPORT_EXPORT("report:export"),

    // ── Marketing ────────────────────────────────────────────────────────────────
    MARKETING_CAMPAIGN_VIEW("marketing:campaign:view"),
    MARKETING_CAMPAIGN_LAUNCH("marketing:campaign:launch"),
    MARKETING_EMAIL_PREVIEW("marketing:email:preview"),
    MARKETING_TEMPLATE_VIEW("marketing:template:view"),
    MARKETING_TEMPLATE_CREATE("marketing:template:create"),
    MARKETING_TEMPLATE_EDIT("marketing:template:edit"),
    MARKETING_TEMPLATE_DELETE("marketing:template:delete"),

    // ── Admin ───────────────────────────────────────────────────────────────────
    ADMIN_FULL_ACCESS("admin:full_access");

    private final String code;

    Permission(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }
}
