package es.terencio.erp.organization.domain.model;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.StoreId;
import es.terencio.erp.shared.domain.identifier.WarehouseId;
import java.time.Instant;

public class Warehouse {
    private final WarehouseId id;
    private final StoreId storeId;
    private String name;
    private String code;
    private final Instant createdAt;

    public Warehouse(WarehouseId id, StoreId storeId, String name, String code, Instant createdAt) {
        if (id == null) throw new InvariantViolationException("Warehouse ID cannot be null");
        if (storeId == null) throw new InvariantViolationException("Warehouse must belong to a store");
        if (name == null || name.isBlank()) throw new InvariantViolationException("Warehouse name cannot be empty");

        this.id = id;
        this.storeId = storeId;
        this.name = name;
        this.code = code;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static Warehouse create(StoreId storeId, String name, String code) {
        return new Warehouse(WarehouseId.create(), storeId, name, code, Instant.now());
    }

    public void updateName(String newName) {
        if (newName == null || newName.isBlank()) throw new InvariantViolationException("Warehouse name cannot be empty");
        this.name = newName;
    }

    public WarehouseId id() { return id; }
    public StoreId storeId() { return storeId; }
    public String name() { return name; }
    public String code() { return code; }
    public Instant createdAt() { return createdAt; }
}
