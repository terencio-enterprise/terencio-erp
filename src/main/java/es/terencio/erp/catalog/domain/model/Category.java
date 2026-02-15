package es.terencio.erp.catalog.domain.model;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.CompanyId;

/**
 * Category entity (product classification).
 */
public class Category {

    private final Long id;
    private final CompanyId companyId;
    private final Long parentId;
    private String name;
    private String color;
    private String imageUrl;
    private boolean active;

    public Category(
            Long id,
            CompanyId companyId,
            Long parentId,
            String name,
            String color,
            String imageUrl,
            boolean active) {

        if (companyId == null)
            throw new InvariantViolationException("Category must belong to a company");
        if (name == null || name.isBlank())
            throw new InvariantViolationException("Category name cannot be empty");

        this.id = id;
        this.companyId = companyId;
        this.parentId = parentId;
        this.name = name;
        this.color = color;
        this.imageUrl = imageUrl;
        this.active = active;
    }

    public static Category create(CompanyId companyId, String name) {
        return new Category(null, companyId, null, name, null, null, true);
    }

    public void updateName(String newName) {
        if (newName == null || newName.isBlank())
            throw new InvariantViolationException("Category name cannot be empty");
        this.name = newName;
    }

    // Getters
    public Long id() {
        return id;
    }

    public CompanyId companyId() {
        return companyId;
    }

    public Long parentId() {
        return parentId;
    }

    public String name() {
        return name;
    }

    public String color() {
        return color;
    }

    public String imageUrl() {
        return imageUrl;
    }

    public boolean isActive() {
        return active;
    }
}
