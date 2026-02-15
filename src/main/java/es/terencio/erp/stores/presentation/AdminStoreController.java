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
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/stores")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStoreController {

    private final ManageStoresUseCase manageStoresUseCase;

    public AdminStoreController(ManageStoresUseCase manageStoresUseCase) {
        this.manageStoresUseCase = manageStoresUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StoreDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success("Stores fetched successfully", manageStoresUseCase.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StoreDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Store fetched successfully", manageStoresUseCase.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StoreDto>> create(@Valid @RequestBody StoreDto request) {
        return ResponseEntity
                .ok(ApiResponse.success("Store created successfully", manageStoresUseCase.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StoreDto>> update(@PathVariable UUID id, @Valid @RequestBody StoreDto request) {
        return ResponseEntity
                .ok(ApiResponse.success("Store updated successfully", manageStoresUseCase.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        manageStoresUseCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Store deleted successfully"));
    }
}