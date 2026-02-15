package es.terencio.erp.organization.application.usecase;

import es.terencio.erp.organization.application.port.in.UpdateStoreSettingsUseCase;
import es.terencio.erp.organization.application.port.out.StoreSettingsRepository;
import es.terencio.erp.organization.domain.model.StoreSettings;
import es.terencio.erp.shared.domain.identifier.StoreId;
import es.terencio.erp.shared.domain.valueobject.Money;
import es.terencio.erp.shared.exception.DomainException;

/**
 * Use case implementation for updating store settings.
 */
public class UpdateStoreSettingsService implements UpdateStoreSettingsUseCase {

    private final StoreSettingsRepository storeSettingsRepository;

    public UpdateStoreSettingsService(StoreSettingsRepository storeSettingsRepository) {
        this.storeSettingsRepository = storeSettingsRepository;
    }

    @Override
    public void execute(UpdateStoreSettingsCommand command) {
        StoreId storeId = new StoreId(command.storeId());

        StoreSettings settings = storeSettingsRepository.findByStoreId(storeId)
                .orElseThrow(() -> new DomainException("Store settings not found"));

        Money requireCustomerAmount = command.requireCustomerForLargeAmount() != null
                ? Money.ofEuros(command.requireCustomerForLargeAmount())
                : Money.ofEuros(1000.0);

        settings.updateSettings(
                command.allowNegativeStock(),
                command.defaultTariffId(),
                command.printTicketAutomatically(),
                requireCustomerAmount);

        storeSettingsRepository.save(settings);
    }
}
