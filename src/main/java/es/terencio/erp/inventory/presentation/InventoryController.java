package es.terencio.erp.inventory.presentation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.inventory.application.port.out.InventoryStockRepository;
import es.terencio.erp.inventory.application.port.out.StockMovementRepository;
import es.terencio.erp.inventory.domain.model.InventoryStock;
import es.terencio.erp.inventory.domain.model.StockMovement;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.identifier.WarehouseId;
import es.terencio.erp.shared.domain.valueobject.Quantity;
import es.terencio.erp.shared.presentation.ApiResponse;
import jakarta.validation.Valid;

/**
 * REST controller for Inventory management.
 */
@RestController
@RequestMapping("/api/v1/inventory")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WAREHOUSE')")
public class InventoryController {

    private final InventoryStockRepository inventoryStockRepository;
    private final StockMovementRepository stockMovementRepository;

    public InventoryController(
            InventoryStockRepository inventoryStockRepository,
            StockMovementRepository stockMovementRepository) {
        this.inventoryStockRepository = inventoryStockRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @GetMapping("/stock")
    public ResponseEntity<ApiResponse<List<StockResponse>>> getStock(
            @RequestParam UUID companyId,
            @RequestParam UUID warehouseId,
            @RequestParam(required = false) Long productId) {

        List<InventoryStock> stocks;

        if (productId != null) {
            stocks = inventoryStockRepository.findByProductId(new ProductId(productId));
        } else {
            stocks = inventoryStockRepository.findByCompanyIdAndWarehouse(
                    new CompanyId(companyId),
                    new WarehouseId(warehouseId));
        }

        List<StockResponse> response = stocks.stream()
                .map(s -> new StockResponse(
                        s.productId().value(),
                        s.warehouseId().value(),
                        s.quantityOnHand().value(),
                        s.lastUpdatedAt()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Stock fetched successfully", response));
    }

    @GetMapping("/products/{productId}/movements")
    public ResponseEntity<ApiResponse<List<MovementResponse>>> getProductMovements(@PathVariable Long productId) {
        List<StockMovement> movements = stockMovementRepository.findByProduct(new ProductId(productId));

        List<MovementResponse> response = movements.stream()
                .map(m -> new MovementResponse(
                        m.id(),
                        m.type().name(),
                        m.quantity().value(),
                        m.previousBalance().value(),
                        m.newBalance().value(),
                        m.reason(),
                        m.createdAt()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Stock movements fetched successfully", response));
    }

    @PostMapping("/adjustments")
    public ResponseEntity<ApiResponse<AdjustmentResponse>> createAdjustment(
            @Valid @RequestBody AdjustmentRequest request) {
        ProductId productId = new ProductId(request.productId());
        WarehouseId warehouseId = new WarehouseId(request.warehouseId());
        CompanyId companyId = new CompanyId(request.companyId());

        // Get current stock or initialize
        InventoryStock stock = inventoryStockRepository.findByProductAndWarehouse(productId, warehouseId)
                .orElse(InventoryStock.initialize(productId, warehouseId, companyId, Quantity.zero()));

        Quantity previousBalance = stock.quantityOnHand();
        Quantity adjustment = Quantity.of(request.adjustmentQuantity());
        Quantity newBalance = previousBalance.add(adjustment);

        // Create stock movement
        StockMovement movement = StockMovement.forAdjustment(
                productId,
                warehouseId,
                adjustment,
                previousBalance,
                request.reason(),
                null);
        stockMovementRepository.save(movement);

        // Update stock projection
        stock.adjustQuantity(adjustment);
        inventoryStockRepository.save(stock);

        return ResponseEntity.ok(ApiResponse.success("Stock adjustment created successfully", new AdjustmentResponse(
                productId.value(),
                previousBalance.value(),
                newBalance.value())));
    }

    // ==================== RECORDS ====================

    public record StockResponse(Long productId, UUID warehouseId, BigDecimal quantityOnHand, Instant lastUpdatedAt) {
    }

    public record MovementResponse(Long id, String type, BigDecimal quantity, BigDecimal previousBalance,
            BigDecimal newBalance, String reason, Instant createdAt) {
    }

    public record AdjustmentRequest(UUID companyId, Long productId, UUID warehouseId,
            BigDecimal adjustmentQuantity, String reason) {
    }

    public record AdjustmentResponse(Long productId, BigDecimal previousBalance, BigDecimal newBalance) {
    }
}
