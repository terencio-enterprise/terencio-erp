package es.terencio.erp.employees.domain.model;

import java.time.Instant;
import java.util.UUID;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.UserId;
import es.terencio.erp.shared.domain.valueobject.Email;

public class Employee {

    private final UserId id;
    private final UUID uuid;
    private final UUID organizationId;
    private String username;
    private Email email;
    private String fullName;
    private UUID lastActiveCompanyId;
    private UUID lastActiveStoreId;
    private boolean active;
    private Instant lastLoginAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public Employee(
            UserId id, UUID uuid, UUID organizationId, String username, Email email,
            String fullName, UUID lastActiveCompanyId, UUID lastActiveStoreId,
            boolean active, Instant lastLoginAt, Instant createdAt, Instant updatedAt) {

        if (uuid == null)
            throw new InvariantViolationException("Employee UUID cannot be null");
        if (organizationId == null)
            throw new InvariantViolationException("Organization ID cannot be null");
        if (username == null || username.isBlank())
            throw new InvariantViolationException("Username cannot be empty");

        this.id = id;
        this.uuid = uuid;
        this.organizationId = organizationId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.lastActiveCompanyId = lastActiveCompanyId;
        this.lastActiveStoreId = lastActiveStoreId;
        this.active = active;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Employee create(UUID organizationId, String username, Email email, String fullName) {
        return new Employee(null, UUID.randomUUID(), organizationId, username, email, fullName, null, null, true, null,
                Instant.now(), Instant.now());
    }

    public void updateProfile(String fullName, Email email) {
        this.fullName = fullName;
        this.email = email;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    // Getters
    public UserId id() {
        return id;
    }

    public UUID uuid() {
        return uuid;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public String username() {
        return username;
    }

    public Email email() {
        return email;
    }

    public String fullName() {
        return fullName;
    }

    public UUID lastActiveCompanyId() {
        return lastActiveCompanyId;
    }

    public UUID lastActiveStoreId() {
        return lastActiveStoreId;
    }

    public boolean isActive() {
        return active;
    }

    public Instant lastLoginAt() {
        return lastLoginAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}