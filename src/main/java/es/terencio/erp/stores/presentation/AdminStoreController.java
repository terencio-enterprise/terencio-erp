package es.terencio.erp.stores.presentation;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.shared.presentation.ApiResponse;
import es.terencio.erp.stores.application.dto.StoreDto;
import es.terencio.erp.stores.application.port.in.ManageStoresUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/stores")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Stores", description = "Administrative store CRUD endpoints")
public class AdminStoreController {

    private final ManageStoresUseCase manageStoresUseCase;

    public AdminStoreController(ManageStoresUseCase manageStoresUseCase) {
        this.manageStoresUseCase = manageStoresUseCase;
    }

    @GetMapping
    @Operation(summary = "List stores", description = "Returns all stores")
    public ResponseEntity<ApiResponse<List<StoreDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success("Stores fetched successfully", manageStoresUseCase.listAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get store", description = "Returns one store by identifier")
    public ResponseEntity<ApiResponse<StoreDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Store fetched successfully", manageStoresUseCase.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Create store", description = "Creates a new store")
    public ResponseEntity<ApiResponse<StoreDto>> create(@Valid @RequestBody StoreDto request) {
        return ResponseEntity
                .ok(ApiResponse.success("Store created successfully", manageStoresUseCase.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update store", description = "Updates an existing store")
    public ResponseEntity<ApiResponse<StoreDto>> update(@PathVariable UUID id, @Valid @RequestBody StoreDto request) {
        return ResponseEntity
                .ok(ApiResponse.success("Store updated successfully", manageStoresUseCase.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete store", description = "Deletes a store by identifier")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        manageStoresUseCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Store deleted successfully"));
    }
}
