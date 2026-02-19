package es.terencio.erp.organization.infrastructure.in.web;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.auth.domain.model.Permission;
import es.terencio.erp.auth.infrastructure.config.security.aop.RequiresPermission;
import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateStoreCommand;
import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateStoreResult;
import es.terencio.erp.organization.application.dto.OrganizationCommands.UpdateStoreSettingsCommand;
import es.terencio.erp.organization.application.port.in.CreateStoreUseCase;
import es.terencio.erp.organization.application.port.in.DeleteStoreUseCase;
import es.terencio.erp.organization.application.port.in.UpdateStoreSettingsUseCase;
import es.terencio.erp.organization.application.port.out.StoreRepository;
import es.terencio.erp.organization.application.port.out.StoreSettingsRepository;
import es.terencio.erp.organization.domain.model.Store;
import es.terencio.erp.organization.domain.model.StoreSettings;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.StoreId;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/stores")
@Tag(name = "Stores", description = "Store management and store settings endpoints")
public class StoreController {

    private final CreateStoreUseCase createStoreUseCase;
    private final UpdateStoreSettingsUseCase updateStoreSettingsUseCase;
    private final DeleteStoreUseCase deleteStoreUseCase;
    private final StoreRepository storeRepository;
    private final StoreSettingsRepository storeSettingsRepository;

    public StoreController(CreateStoreUseCase createStoreUseCase, UpdateStoreSettingsUseCase updateStoreSettingsUseCase,
            DeleteStoreUseCase deleteStoreUseCase, StoreRepository storeRepository, StoreSettingsRepository storeSettingsRepository) {
        this.createStoreUseCase = createStoreUseCase;
        this.updateStoreSettingsUseCase = updateStoreSettingsUseCase;
        this.deleteStoreUseCase = deleteStoreUseCase;
        this.storeRepository = storeRepository;
        this.storeSettingsRepository = storeSettingsRepository;
    }

    @PostMapping
    @Operation(summary = "Create store")
    @RequiresPermission(permission = Permission.ORGANIZATION_STORE_CREATE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CreateStoreResult>> createStore(@Valid @RequestBody CreateStoreCommand command) {
        return ResponseEntity.ok(ApiResponse.success("Store created successfully", createStoreUseCase.execute(command)));
    }

    @GetMapping
    @Operation(summary = "List stores by company")
    @RequiresPermission(permission = Permission.ORGANIZATION_STORE_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<List<StoreResponse>>> listStores(@RequestParam UUID companyId) {
        List<StoreResponse> response = storeRepository.findByCompanyId(new CompanyId(companyId)).stream().map(StoreController::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success("Stores fetched successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get store")
    @RequiresPermission(permission = Permission.ORGANIZATION_STORE_VIEW, scope = AccessScope.STORE, targetIdParam = "id")
    public ResponseEntity<ApiResponse<StoreResponse>> getStore(@PathVariable UUID id) {
        Store store = storeRepository.findById(new StoreId(id)).orElseThrow(() -> new es.terencio.erp.shared.exception.DomainException("Store not found: " + id));
        return ResponseEntity.ok(ApiResponse.success("Store fetched successfully", toResponse(store)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete store")
    @RequiresPermission(permission = Permission.ORGANIZATION_STORE_DELETE, scope = AccessScope.STORE, targetIdParam = "id")
    public ResponseEntity<ApiResponse<Void>> deleteStore(@PathVariable UUID id) {
        deleteStoreUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.success("Store deleted successfully"));
    }

    @GetMapping("/{id}/settings")
    @Operation(summary = "Get store settings")
    @RequiresPermission(permission = Permission.ORGANIZATION_STORE_VIEW, scope = AccessScope.STORE, targetIdParam = "id")
    public ResponseEntity<ApiResponse<StoreSettingsResponse>> getStoreSettings(@PathVariable UUID id) {
        StoreSettings settings = storeSettingsRepository.findByStoreId(new StoreId(id)).orElseThrow(() -> new es.terencio.erp.shared.exception.DomainException("Store settings not found for store: " + id));
        return ResponseEntity.ok(ApiResponse.success("Store settings fetched successfully", new StoreSettingsResponse(settings.allowNegativeStock(), settings.defaultTariffId(), settings.printTicketAutomatically(), settings.requireCustomerForLargeAmount().cents())));
    }

    @PutMapping("/{id}/settings")
    @Operation(summary = "Update store settings")
    @RequiresPermission(permission = Permission.ORGANIZATION_STORE_UPDATE, scope = AccessScope.STORE, targetIdParam = "id")
    public ResponseEntity<ApiResponse<Void>> updateStoreSettings(@Parameter(description = "Store identifier") @PathVariable UUID id, @Valid @RequestBody UpdateStoreSettingsCommand command) {
        updateStoreSettingsUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Store settings updated successfully"));
    }

    private static StoreResponse toResponse(Store s) {
        return new StoreResponse(s.id().value(), s.code(), s.name(), s.address() != null ? s.address().street() : null, s.isActive());
    }

    public record StoreResponse(UUID id, String code, String name, String address, boolean active) {}
    public record StoreSettingsResponse(boolean allowNegativeStock, Long defaultTariffId, boolean printTicketAutomatically, long requireCustomerForLargeAmountCents) {}
}
