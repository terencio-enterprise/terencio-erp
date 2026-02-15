package es.terencio.erp.shared.domain.tenant;

import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.StoreId;

import java.util.Objects;

/**
 * Value Object representing the multi-tenant context.
 * All domain operations must be scoped to a Company; some are also scoped to a
 * Store.
 */
public final class TenantContext {

    private final CompanyId companyId;
    private final StoreId storeId; // Optional - null means company-wide

    private TenantContext(CompanyId companyId, StoreId storeId) {
        if (companyId == null) {
            throw new IllegalArgumentException("CompanyId cannot be null");
        }
        this.companyId = companyId;
        this.storeId = storeId;
    }

    public static TenantContext of(CompanyId companyId) {
        return new TenantContext(companyId, null);
    }

    public static TenantContext of(CompanyId companyId, StoreId storeId) {
        return new TenantContext(companyId, storeId);
    }

    public CompanyId companyId() {
        return companyId;
    }

    public StoreId storeId() {
        return storeId;
    }

    public boolean hasStore() {
        return storeId != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TenantContext that = (TenantContext) o;
        return Objects.equals(companyId, that.companyId) && Objects.equals(storeId, that.storeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, storeId);
    }

    @Override
    public String toString() {
        return hasStore() ? "TenantContext{company=" + companyId + ", store=" + storeId + "}"
                : "TenantContext{company=" + companyId + "}";
    }
}
