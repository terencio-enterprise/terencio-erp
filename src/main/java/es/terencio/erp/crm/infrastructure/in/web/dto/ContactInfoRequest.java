package es.terencio.erp.crm.infrastructure.in.web.dto;

import es.terencio.erp.crm.domain.model.ContactInfo;
import es.terencio.erp.shared.domain.valueobject.Email;

public record ContactInfoRequest(
    String email,
    String phone,
    String address,
    String zipCode,
    String city,
    String country
) {
    public ContactInfo toDomain() {
        return new ContactInfo(
            email != null ? Email.of(email) : null,
            phone,
            address,
            zipCode,
            city,
            country
        );
    }
}