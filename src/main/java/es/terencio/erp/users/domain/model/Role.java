package es.terencio.erp.users.domain.model;

public enum Role {
    ADMIN("Administrator", "Full access to everything"),
    MANAGER("Store Manager", "Can manage stock and overrides"),
    CASHIER("Cashier", "POS sales only"),
    WAREHOUSE("Warehouse Staff", "Stock management only");

    private final String label;
    private final String description;

    Role(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
}