package es.terencio.erp.crm.domain.model;

import es.terencio.erp.shared.domain.valueobject.Email;

public record ContactInfo(
        Email email, 
        String phone, 
        String address, 
        String zipCode, 
        String city, 
        String country
) {
    public static ContactInfo empty() { 
        return new ContactInfo(null, null, null, null, null, "ES"); 
    }
}