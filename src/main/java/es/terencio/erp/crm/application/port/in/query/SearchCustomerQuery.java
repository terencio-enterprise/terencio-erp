package es.terencio.erp.crm.application.port.in.query;

import es.terencio.erp.crm.domain.model.CustomerType;

public record SearchCustomerQuery(
        String search, 
        CustomerType type, 
        Boolean active, 
        int page, 
        int size
) {}