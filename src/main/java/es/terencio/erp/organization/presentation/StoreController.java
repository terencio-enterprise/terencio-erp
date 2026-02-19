package es.terencio.erp.organization.presentation;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.auth.domain.model.Permission;
import es.terencio.erp.auth.infrastructure.security.RequiresPermission;
import es.terencio.erp.organization.application.port.in.CreateStoreUseCase;
import es.terencio.erp.organization.application.port.in.DeleteStoreUseCase;
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
 * REST controller for Store management (Organization domain).
 * Authorization is enforced via {@link RequiresPermission} on each method.
 */
@RestController
@RequestMapping("/api/v1/stores")
@Tag(name = "Stores", description = "Store management and store settings endpoints")
public class StoreController {

        private final CreateStoreUseCase createStoreUseCase;
        private final UpdateStoreSettingsUseCase updateStoreSettingsUseCase;
        private final DeleteStoreUseCase deleteStoreUseCase;
        private final StoreRepository storeRepository;
        private final StoreSettingsRepository storeSettingsRepository;

        public StoreController(
                        CreateStoreUseCase createStoreUseCase,
                        UpdateStoreSettingsUseCase updateStoreSettingsUseCase,
                        DeleteStoreUseCase deleteStoreUseCase,
                        StoreRepository storeRepository,
                        StoreSettingsRepository storeSettingsRepository) {
                this.createStoreUseCase = createStoreUseCase;
                this.updateStoreSettingsUseCase = updateStoreSettingsUseCase;
                this.deleteStoreUseCase = deleteStoreUseCase;
                this.storeRepository = storeRepository;
                this.storeSettingsRepository = storeSettingsRepository;
        }

        // ────────────────────────────────────────────────────────────────────────────
        // CRUD
        // ────────────────────────────────────────────────────────────────────────────

        @PostMapping
        @Operation(summary = "Create store", description = "Creates a new store for a company")
        @RequiresPermission(permission = Permission.ORGANIZATION_STORE_CREATE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
        public ResponseEntity<ApiResponse<CreateStoreResult>> createStore(
                        @Valid @RequestBody CreateStoreCommand command) {
                CreateStoreResult result = createStoreUseCase.execute(command);
                return ResponseEntity.ok(ApiResponse.success("Store created successfully", result));
        }

        @GetMapping
        @Operation(summary = "List stores by company", description = "Returns all stores for a given company")
        @RequiresPermission(permission = Permission.ORGANIZATION_STORE_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
        public ResponseEntity<ApiResponse<List<StoreResponse>>> listStores(@RequestParam UUID companyId) {
                List<Store> stores = storeRepository.findByCompanyId(new CompanyId(companyId));
                List<StoreResponse> response = stores.stream()
                                .map(StoreController::toResponse)
                                .toList();
                return ResponseEntity.ok(ApiResponse.success("Stores fetched successfully", response));
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get store", description = "Returns one store by identifier")
        @RequiresPermission(permission = Permission.ORGANIZATION_STORE_VIEW, scope = AccessScope.STORE, targetIdParam = "id")
        public ResponseEntity<ApiResponse<StoreResponse>> getStore(@PathVariable UUID id) {
                Store store = storeRepository.findById(new StoreId(id))
                                .orElseThrow(() -> new es.terencio.erp.shared.exception.DomainException(
                                                "Store not found: " + id));
                return ResponseEntity.ok(ApiResponse.success("Store fetched successfully", toResponse(store)));
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Delete store", description = "Soft-deletes a store (fails if it has active personnel or devices)")
        @RequiresPermission(permission = Permission.ORGANIZATION_STORE_DELETE, scope = AccessScope.STORE, targetIdParam = "id")
        public ResponseEntity<ApiResponse<Void>> deleteStore(@PathVariable UUID id) {
                deleteStoreUseCase.execute(id);
                return ResponseEntity.ok(ApiResponse.success("Store deleted successfully"));
        }

        // ────────────────────────────────────────────────────────────────────────────
        // Settings
        // ────────────────────────────────────────────────────────────────────────────

        @GetMapping("/{id}/settings")
        @Operation(summary = "Get store settings", description = "Returns configuration settings for one store")
        @RequiresPermission(permission = Permission.ORGANIZATION_STORE_VIEW, scope = AccessScope.STORE, targetIdParam = "id")
        public ResponseEntity<ApiResponse<StoreSettingsResponse>> getStoreSettings(@PathVariable UUID id) {
                StoreSettings settings = storeSettingsRepository.findByStoreId(new StoreId(id))
                                .orElseThrow(() -> new es.terencio.erp.shared.exception.DomainException(
                                                "Store settings not found for store: " + id));

                return ResponseEntity.ok(ApiResponse.success("Store settings fetched successfully",
                                new StoreSettingsResponse(
                                                settings.allowNegativeStock(),
                                                settings.defaultTariffId(),
                                                settings.printTicketAutomatically(),
                                                settings.requireCustomerForLargeAmount().cents())));
        }

        @PutMapping("/{id}/settings")
        @Operation(summary = "Update store settings", description = "Updates store operational settings")
        @RequiresPermission(permission = Permission.ORGANIZATION_STORE_UPDATE, scope = AccessScope.STORE, targetIdParam = "id")
        public ResponseEntity<ApiResponse<Void>> updateStoreSettings(
                        @Parameter(description = "Store identifier") @PathVariable UUID id,
                        @Valid @RequestBody UpdateStoreSettingsCommand command) {
                updateStoreSettingsUseCase.execute(command);
                return ResponseEntity.ok(ApiResponse.success("Store settings updated successfully"));
        }

        // ────────────────────────────────────────────────────────────────────────────
        // Internal records
        // ────────────────────────────────────────────────────────────────────────────

        private static StoreResponse toResponse(Store s) {
                return new StoreResponse(
                                s.id().value(),
                                s.code(),
                                s.name(),
                                s.address() != null ? s.address().street() : null,
                                s.isActive());
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
