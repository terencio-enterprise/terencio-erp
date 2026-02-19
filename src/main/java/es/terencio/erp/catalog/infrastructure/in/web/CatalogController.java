package es.terencio.erp.catalog.infrastructure.in.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.auth.domain.model.Permission;
import es.terencio.erp.auth.infrastructure.config.security.aop.RequiresPermission;
import es.terencio.erp.catalog.application.port.in.ListTaxesUseCase;
import es.terencio.erp.catalog.application.port.in.SearchProductsUseCase;
import es.terencio.erp.catalog.application.port.out.*;
import es.terencio.erp.catalog.domain.model.*;
import es.terencio.erp.shared.domain.identifier.*;
import es.terencio.erp.shared.domain.valueobject.Money;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/catalog")
@Tag(name = "Catalog", description = "Catalog management endpoints")
public class CatalogController {

    private final ListTaxesUseCase listTaxesUseCase;
    private final SearchProductsUseCase searchProductsUseCase;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final TariffRepository tariffRepository;
    private final ProductPriceRepository productPriceRepository;

    public CatalogController(ListTaxesUseCase listTaxesUseCase, SearchProductsUseCase searchProductsUseCase,
            CategoryRepository categoryRepository, ProductRepository productRepository,
            TariffRepository tariffRepository, ProductPriceRepository productPriceRepository) {
        this.listTaxesUseCase = listTaxesUseCase;
        this.searchProductsUseCase = searchProductsUseCase;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.tariffRepository = tariffRepository;
        this.productPriceRepository = productPriceRepository;
    }

    @GetMapping("/taxes")
    @Operation(summary = "List taxes")
    @RequiresPermission(permission = Permission.PRODUCT_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<List<TaxResponse>>> listTaxes(@RequestParam UUID companyId) {
        List<TaxResponse> response = listTaxesUseCase.listTaxes(new CompanyId(companyId)).stream()
                .map(t -> new TaxResponse(t.id(), t.name(), t.rate().rate(), t.surcharge().rate())).toList();
        return ResponseEntity.ok(ApiResponse.success("Taxes fetched successfully", response));
    }

    @GetMapping("/categories")
    @Operation(summary = "List categories")
    @RequiresPermission(permission = Permission.PRODUCT_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> listCategories(@RequestParam UUID companyId) {
        List<CategoryResponse> response = categoryRepository.findByCompanyId(new CompanyId(companyId)).stream()
                .map(c -> new CategoryResponse(c.id(), c.parentId(), c.name(), c.color(), c.isActive())).toList();
        return ResponseEntity.ok(ApiResponse.success("Categories fetched successfully", response));
    }

    @PostMapping("/categories")
    @Operation(summary = "Create category")
    @RequiresPermission(permission = Permission.PRODUCT_CREATE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        Category saved = categoryRepository.save(Category.create(new CompanyId(request.companyId()), request.name()));
        return ResponseEntity.ok(ApiResponse.success("Category created successfully", new CategoryResponse(saved.id(), saved.parentId(), saved.name(), saved.color(), saved.isActive())));
    }

    @GetMapping("/products")
    @Operation(summary = "Search products")
    @RequiresPermission(permission = Permission.PRODUCT_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestParam UUID companyId, @RequestParam(required = false) String name,
            @RequestParam(required = false) String reference, @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        List<ProductResponse> response = searchProductsUseCase.search(new CompanyId(companyId), name, reference, categoryId, page, size).stream()
                .map(p -> new ProductResponse(p.id().value(), p.reference(), p.name(), p.shortName(), p.categoryId(), p.taxId(), p.type().name(), p.isActive())).toList();
        return ResponseEntity.ok(ApiResponse.success("Products fetched successfully", response));
    }

    @PostMapping("/products")
    @Operation(summary = "Create product")
    @RequiresPermission(permission = Permission.PRODUCT_CREATE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        CompanyId companyId = new CompanyId(request.companyId());
        if (productRepository.existsByCompanyAndReference(companyId, request.reference())) throw new RuntimeException("Product reference already exists");
        Product saved = productRepository.save(Product.create(companyId, request.reference(), request.name(), request.taxId(), ProductType.valueOf(request.type())));
        return ResponseEntity.ok(ApiResponse.success("Product created", new ProductResponse(saved.id().value(), saved.reference(), saved.name(), saved.shortName(), saved.categoryId(), saved.taxId(), saved.type().name(), saved.isActive())));
    }

    public record TaxResponse(Long id, String name, BigDecimal rate, BigDecimal surcharge) {}
    public record CategoryResponse(Long id, Long parentId, String name, String color, boolean active) {}
    public record CreateCategoryRequest(UUID companyId, String name) {}
    public record ProductResponse(Long id, String reference, String name, String shortName, Long categoryId, Long taxId, String type, boolean active) {}
    public record CreateProductRequest(UUID companyId, String reference, String name, String shortName, Long taxId, String type) {}
}
