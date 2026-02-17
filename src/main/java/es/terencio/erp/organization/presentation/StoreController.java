package es.terencio.erp.organization.presentation;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.organization.application.port.in.CreateStoreUseCase;
import es.terencio.erp.organization.application.port.in.UpdateStoreSettingsUseCase;
import es.terencio.erp.organization.application.port.out.StoreRepository;
import es.terencio.erp.organization.application.port.out.StoreSettingsRepository;
import es.terencio.erp.organization.application.usecase.CreateStoreCommand;
import es.terencio.erp.organization.application.usecase.CreateStoreResult;
import es.terencio.erp.organization.application.usecase.UpdateStoreSettingsCommand;
import es.terencio.erp.organization.domain.model.Store;
import es.terencio.erp.organization.domain.model.StoreSettings;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.StoreId;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST controller for Store management (Organization module).
 */
@RestController
@RequestMapping("/api/v1/stores")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Stores", description = "Store management and store settings endpoints")
public class StoreController {

    private final CreateStoreUseCase createStoreUseCase;
    private final UpdateStoreSettingsUseCase updateStoreSettingsUseCase;
    private final StoreRepository storeRepository;
    private final StoreSettingsRepository storeSettingsRepository;

    public StoreController(
            CreateStoreUseCase createStoreUseCase,
            UpdateStoreSettingsUseCase updateStoreSettingsUseCase,
            StoreRepository storeRepository,
            StoreSettingsRepository storeSettingsRepository) {
        this.createStoreUseCase = createStoreUseCase;
        this.updateStoreSettingsUseCase = updateStoreSettingsUseCase;
        this.storeRepository = storeRepository;
        this.storeSettingsRepository = storeSettingsRepository;
    }

    @PostMapping
    @Operation(summary = "Create store", description = "Creates a new store for a company")
    public ResponseEntity<ApiResponse<CreateStoreResult>> createStore(@Valid @RequestBody CreateStoreCommand command) {
        CreateStoreResult result = createStoreUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Store created successfully", result));
    }

    @GetMapping
    @Operation(summary = "List stores", description = "Returns stores for a given company")
    public ResponseEntity<ApiResponse<List<StoreResponse>>> listStores(@RequestParam UUID companyId) {
        List<Store> stores = storeRepository.findByCompanyId(new CompanyId(companyId));
        List<StoreResponse> response = stores.stream()
                .map(s -> new StoreResponse(
                        s.id().value(),
                        s.code(),
                        s.name(),
                        s.address() != null ? s.address().street() : null,
                        s.isActive()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Stores fetched successfully", response));
    }

    @GetMapping("/{id}/settings")
    @Operation(summary = "Get store settings", description = "Returns configuration settings for one store")
    public ResponseEntity<ApiResponse<StoreSettingsResponse>> getStoreSettings(@PathVariable UUID id) {
        StoreSettings settings = storeSettingsRepository.findByStoreId(new StoreId(id))
                .orElseThrow(() -> new RuntimeException("Store settings not found"));

        return ResponseEntity.ok(ApiResponse.success("Store settings fetched successfully", new StoreSettingsResponse(
                settings.allowNegativeStock(),
                settings.defaultTariffId(),
                settings.printTicketAutomatically(),
                settings.requireCustomerForLargeAmount().cents())));
    }

    @PutMapping("/{id}/settings")
    @Operation(summary = "Update store settings", description = "Updates store operational settings")
    public ResponseEntity<ApiResponse<Void>> updateStoreSettings(
            @Parameter(description = "Store identifier") @PathVariable UUID id,
            @Valid @RequestBody UpdateStoreSettingsCommand command) {
        updateStoreSettingsUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Store settings updated successfully"));
    }

    public record StoreResponse(
            UUID id,
            String code,
            String name,
            String address,
            boolean active) {
    }

    public record StoreSettingsResponse(
            boolean allowNegativeStock,
            Long defaultTariffId,
            boolean printTicketAutomatically,
            long requireCustomerForLargeAmountCents) {
    }
}
