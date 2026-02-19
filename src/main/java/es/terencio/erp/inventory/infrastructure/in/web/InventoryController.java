package es.terencio.erp.inventory.infrastructure.in.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.auth.domain.model.Permission;
import es.terencio.erp.auth.infrastructure.config.security.aop.RequiresPermission;
import es.terencio.erp.inventory.application.port.out.*;
import es.terencio.erp.inventory.domain.model.*;
import es.terencio.erp.shared.domain.identifier.*;
import es.terencio.erp.shared.domain.valueobject.Quantity;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory", description = "Inventory stock, movements and adjustments")
public class InventoryController {

    private final InventoryStockRepository inventoryStockRepository;
    private final StockMovementRepository stockMovementRepository;

    public InventoryController(InventoryStockRepository inventoryStockRepository, StockMovementRepository stockMovementRepository) {
        this.inventoryStockRepository = inventoryStockRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @GetMapping("/stock")
    @Operation(summary = "Get stock")
    @RequiresPermission(permission = Permission.INVENTORY_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<List<StockResponse>>> getStock(
            @RequestParam UUID companyId, @RequestParam UUID warehouseId, @RequestParam(required = false) Long productId) {
        List<InventoryStock> stocks = (productId != null) 
            ? inventoryStockRepository.findByProductId(new ProductId(productId))
            : inventoryStockRepository.findByCompanyIdAndWarehouse(new CompanyId(companyId), new WarehouseId(warehouseId));
        List<StockResponse> response = stocks.stream().map(s -> new StockResponse(s.productId().value(), s.warehouseId().value(), s.quantityOnHand().value(), s.lastUpdatedAt())).toList();
        return ResponseEntity.ok(ApiResponse.success("Stock fetched", response));
    }

    @PostMapping("/adjustments")
    @Operation(summary = "Create stock adjustment")
    @RequiresPermission(permission = Permission.INVENTORY_UPDATE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<AdjustmentResponse>> createAdjustment(@Valid @RequestBody AdjustmentRequest request) {
        ProductId productId = new ProductId(request.productId());
        WarehouseId warehouseId = new WarehouseId(request.warehouseId());
        CompanyId companyId = new CompanyId(request.companyId());

        InventoryStock stock = inventoryStockRepository.findByProductAndWarehouse(productId, warehouseId).orElse(InventoryStock.initialize(productId, warehouseId, companyId, Quantity.zero()));
        Quantity previousBalance = stock.quantityOnHand();
        Quantity adjustment = Quantity.of(request.adjustmentQuantity());
        Quantity newBalance = previousBalance.add(adjustment);

        StockMovement movement = StockMovement.forAdjustment(productId, warehouseId, adjustment, previousBalance, request.reason(), null);
        stockMovementRepository.save(movement);

        stock.adjustQuantity(adjustment);
        inventoryStockRepository.save(stock);

        return ResponseEntity.ok(ApiResponse.success("Adjustment successful", new AdjustmentResponse(productId.value(), previousBalance.value(), newBalance.value())));
    }

    public record StockResponse(Long productId, UUID warehouseId, BigDecimal quantityOnHand, Instant lastUpdatedAt) {}
    public record AdjustmentRequest(UUID companyId, Long productId, UUID warehouseId, BigDecimal adjustmentQuantity, String reason) {}
    public record AdjustmentResponse(Long productId, BigDecimal previousBalance, BigDecimal newBalance) {}
}
