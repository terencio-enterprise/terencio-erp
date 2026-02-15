package es.terencio.erp.sales.application.usecase;

import es.terencio.erp.sales.application.port.in.CreateSaleDraftUseCase;
import es.terencio.erp.sales.application.port.out.SaleRepository;
import es.terencio.erp.sales.domain.model.DocumentType;
import es.terencio.erp.sales.domain.model.Sale;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.DeviceId;
import es.terencio.erp.shared.domain.identifier.StoreId;
import es.terencio.erp.shared.domain.identifier.UserId;

/**
 * Service for creating sale drafts.
 */
public class CreateSaleDraftService implements CreateSaleDraftUseCase {

    private final SaleRepository saleRepository;

    public CreateSaleDraftService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    @Override
    public CreateSaleDraftResult execute(CreateSaleDraftCommand command) {
        CompanyId companyId = new CompanyId(command.companyId());
        StoreId storeId = new StoreId(command.storeId());
        DeviceId deviceId = new DeviceId(command.deviceId());
        UserId userId = new UserId(command.userId());

        DocumentType docType = command.documentType() != null
                ? DocumentType.valueOf(command.documentType())
                : DocumentType.SIMPLIFIED;

        Sale sale = Sale.createDraft(companyId, storeId, deviceId, userId, docType);

        if (command.customerId() != null) {
            // Associate customer (would need setter in Sale or pass in constructor)
        }

        Sale saved = saleRepository.save(sale);

        return new CreateSaleDraftResult(
                saved.uuid().value(),
                saved.status().name());
    }
}
