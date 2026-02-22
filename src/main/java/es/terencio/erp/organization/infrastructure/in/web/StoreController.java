package es.terencio.erp.organization.infrastructure.in.web;

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
import es.terencio.erp.auth.infrastructure.config.security.aop.RequiresPermission;
import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateStoreCommand;
import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateStoreResult;
import es.terencio.erp.organization.application.dto.OrganizationCommands.UpdateStoreSettingsCommand;
import es.terencio.erp.organization.application.port.in.StoreUseCase;
import es.terencio.erp.organization.domain.model.Store;
import es.terencio.erp.organization.domain.model.StoreSettings;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/stores")
@Tag(name = "Stores", description = "Store management and store settings endpoints")
public class StoreController {

    private final StoreUseCase storeUseCase;

    public StoreController(StoreUseCase storeUseCase) {
        this.storeUseCase = storeUseCase;
    }

    @PostMapping
    @Operation(summary = "Create store")
    @RequiresPermission(permission = Permission.ORGANIZATION_STORE_CREATE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CreateStoreResult>> createStore(@Valid @RequestBody CreateStoreCommand command) {
        return ResponseEntity.ok(ApiResponse.success("Store created successfully", storeUseCase.create(command)));
    }

    @GetMapping
    @Operation(summary = "List stores by company")
    @RequiresPermission(permission = Permission.ORGANIZATION_STORE_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<List<StoreResponse>>> listStores(@RequestParam UUID companyId) {
        List<StoreResponse> response = storeUseCase.getAllByCompany(companyId).stream().map(StoreController::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success("Stores fetched successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get store")
    @RequiresPermission(permission = Permission.ORGANIZATION_STORE_VIEW, scope = AccessScope.STORE, targetIdParam = "id")
    public ResponseEntity<ApiResponse<StoreResponse>> getStore(@PathVariable UUID id) {
        Store store = storeUseCase.getById(id);
        return ResponseEntity.ok(ApiResponse.success("Store fetched successfully", toResponse(store)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete store")
    @RequiresPermission(permission = Permission.ORGANIZATION_STORE_DELETE, scope = AccessScope.STORE, targetIdParam = "id")
    public ResponseEntity<ApiResponse<Void>> deleteStore(@PathVariable UUID id) {
        storeUseCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Store deleted successfully"));
    }

    @GetMapping("/{id}/settings")
    @Operation(summary = "Get store settings")
    @RequiresPermission(permission = Permission.ORGANIZATION_STORE_VIEW, scope = AccessScope.STORE, targetIdParam = "id")
    public ResponseEntity<ApiResponse<StoreSettingsResponse>> getStoreSettings(@PathVariable UUID id) {
        StoreSettings settings = storeUseCase.getSettings(id);
        return ResponseEntity.ok(ApiResponse.success(
            "Store settings fetched successfully", 
            new StoreSettingsResponse(settings.allowNegativeStock(), settings.defaultTariffId(), settings.printTicketAutomatically(), settings.requireCustomerForLargeAmount().cents())
        ));
    }

    @PutMapping("/{id}/settings")
    @Operation(summary = "Update store settings")
    @RequiresPermission(permission = Permission.ORGANIZATION_STORE_UPDATE, scope = AccessScope.STORE, targetIdParam = "id")
    public ResponseEntity<ApiResponse<Void>> updateStoreSettings(
            @Parameter(description = "Store identifier") @PathVariable UUID id, 
            @Valid @RequestBody UpdateStoreSettingsCommand command) {
        
        // Merge the PathVariable ID into the Command to ensure consistent UUID behavior
        UpdateStoreSettingsCommand commandWithId = new UpdateStoreSettingsCommand(
            id,
            command.allowNegativeStock(),
            command.defaultTariffId(),
            command.printTicketAutomatically(),
            command.requireCustomerForLargeAmount()
        );
        
        storeUseCase.updateSettings(commandWithId);
        return ResponseEntity.ok(ApiResponse.success("Store settings updated successfully"));
    }

    private static StoreResponse toResponse(Store s) {
        return new StoreResponse(s.id().value(), s.code(), s.name(), s.address() != null ? s.address().street() : null, s.isActive());
    }

    public record StoreResponse(UUID id, String code, String name, String address, boolean active) {}
    public record StoreSettingsResponse(boolean allowNegativeStock, Long defaultTariffId, boolean printTicketAutomatically, long requireCustomerForLargeAmountCents) {}
}