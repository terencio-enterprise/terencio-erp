package es.terencio.erp.organization.domain.model;

import java.time.Instant;
import java.time.ZoneId;
import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.StoreId;
import es.terencio.erp.shared.domain.valueobject.TaxId;

public class Store {
    private final StoreId id;
    private final CompanyId companyId;
    private String code;
    private String name;
    private Address address;
    private TaxId taxId;
    private ZoneId timezone;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;
    private long version;

    public Store(StoreId id, CompanyId companyId, String code, String name, Address address, TaxId taxId,
            ZoneId timezone, boolean active, Instant createdAt, Instant updatedAt, long version) {
        if (id == null) throw new InvariantViolationException("Store ID cannot be null");
        if (companyId == null) throw new InvariantViolationException("Store must belong to a company");
        if (code == null || code.isBlank()) throw new InvariantViolationException("Store code cannot be empty");
        if (name == null || name.isBlank()) throw new InvariantViolationException("Store name cannot be empty");

        this.id = id;
        this.companyId = companyId;
        this.code = code;
        this.name = name;
        this.address = address;
        this.taxId = taxId;
        this.timezone = timezone != null ? timezone : ZoneId.of("Europe/Madrid");
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.version = version;
    }

    public static Store create(CompanyId companyId, String code, String name, Address address, TaxId taxId) {
        return new Store(StoreId.create(), companyId, code, name, address, taxId, ZoneId.of("Europe/Madrid"),
                true, Instant.now(), Instant.now(), 1);
    }

    public void updateName(String newName) {
        if (newName == null || newName.isBlank()) throw new InvariantViolationException("Store name cannot be empty");
        this.name = newName;
        this.updatedAt = Instant.now();
    }

    public void updateAddress(Address newAddress) { this.address = newAddress; this.updatedAt = Instant.now(); }
    public void updateTaxId(TaxId newTaxId) { this.taxId = newTaxId; this.updatedAt = Instant.now(); }
    public void activate() { this.active = true; this.updatedAt = Instant.now(); }
    public void deactivate() { this.active = false; this.updatedAt = Instant.now(); }

    public StoreId id() { return id; }
    public CompanyId companyId() { return companyId; }
    public String code() { return code; }
    public String name() { return name; }
    public Address address() { return address; }
    public TaxId taxId() { return taxId; }
    public ZoneId timezone() { return timezone; }
    public boolean isActive() { return active; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public long version() { return version; }
}
