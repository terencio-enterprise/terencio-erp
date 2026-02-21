package es.terencio.erp.crm.application.port.in.command;

public record ContactInfoCommand(
        String email,
        String phone,
        String address,
        String zipCode,
        String city,
        String country
) {}