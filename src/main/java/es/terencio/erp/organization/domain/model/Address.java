package es.terencio.erp.organization.domain.model;

import java.util.Objects;

/**
 * Value Object representing a physical address.
 */
public final class Address {

    private final String street;
    private final String zipCode;
    private final String city;
    private final String country;

    public Address(String street, String zipCode, String city, String country) {
        this.street = street;
        this.zipCode = zipCode;
        this.city = city;
        this.country = country != null ? country : "ES";
    }

    public static Address of(String street, String zipCode, String city) {
        return new Address(street, zipCode, city, "ES");
    }

    public String street() {
        return street;
    }

    public String zipCode() {
        return zipCode;
    }

    public String city() {
        return city;
    }

    public String country() {
        return country;
    }

    public String formattedAddress() {
        StringBuilder sb = new StringBuilder();
        if (street != null)
            sb.append(street);
        if (zipCode != null) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(zipCode);
        }
        if (city != null) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(city);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Address address = (Address) o;
        return Objects.equals(street, address.street) &&
                Objects.equals(zipCode, address.zipCode) &&
                Objects.equals(city, address.city) &&
                Objects.equals(country, address.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, zipCode, city, country);
    }

    @Override
    public String toString() {
        return formattedAddress();
    }
}
