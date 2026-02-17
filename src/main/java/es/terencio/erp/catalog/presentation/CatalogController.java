package es.terencio.erp.catalog.presentation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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

import es.terencio.erp.catalog.application.port.out.CategoryRepository;
import es.terencio.erp.catalog.application.port.out.ProductPriceRepository;
import es.terencio.erp.catalog.application.port.out.ProductRepository;
import es.terencio.erp.catalog.application.port.out.TariffRepository;
import es.terencio.erp.catalog.application.port.out.TaxRepository;
import es.terencio.erp.catalog.domain.model.Category;
import es.terencio.erp.catalog.domain.model.Product;
import es.terencio.erp.catalog.domain.model.ProductPrice;
import es.terencio.erp.catalog.domain.model.ProductType;
import es.terencio.erp.catalog.domain.model.Tariff;
import es.terencio.erp.catalog.domain.model.Tax;
import es.terencio.erp.catalog.infrastructure.persistence.JdbcProductPersistenceAdapter;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.valueobject.Money;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST controller for Catalog management.
 */
@RestController
@RequestMapping("/api/v1/catalog")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Catalog", description = "Catalog management endpoints for taxes, categories, products and tariffs")
public class CatalogController {

    private final TaxRepository taxRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final TariffRepository tariffRepository;
    private final ProductPriceRepository productPriceRepository;
    private final JdbcProductPersistenceAdapter jdbcProductPersistenceAdapter;

    public CatalogController(
            TaxRepository taxRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            TariffRepository tariffRepository,
            ProductPriceRepository productPriceRepository,
            JdbcProductPersistenceAdapter jdbcProductPersistenceAdapter) {
        this.taxRepository = taxRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.tariffRepository = tariffRepository;
        this.productPriceRepository = productPriceRepository;
        this.jdbcProductPersistenceAdapter = jdbcProductPersistenceAdapter;
    }

    // ==================== TAX ENDPOINTS ====================

    @GetMapping("/taxes")
    @Operation(summary = "List taxes", description = "Returns taxes configured for a company")
    public ResponseEntity<ApiResponse<List<TaxResponse>>> listTaxes(@RequestParam UUID companyId) {
        List<Tax> taxes = taxRepository.findByCompanyId(new CompanyId(companyId));
        List<TaxResponse> response = taxes.stream()
                .map(t -> new TaxResponse(t.id(), t.name(), t.rate().rate(), t.surcharge().rate()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Taxes fetched successfully", response));
    }

    // ==================== CATEGORY ENDPOINTS ====================

    @GetMapping("/categories")
    @Operation(summary = "List categories", description = "Returns product categories for a company")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> listCategories(@RequestParam UUID companyId) {
        List<Category> categories = categoryRepository.findByCompanyId(new CompanyId(companyId));
        List<CategoryResponse> response = categories.stream()
                .map(c -> new CategoryResponse(c.id(), c.parentId(), c.name(), c.color(), c.isActive()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Categories fetched successfully", response));
    }

    @PostMapping("/categories")
    @Operation(summary = "Create category", description = "Creates a new catalog category")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        Category category = Category.create(new CompanyId(request.companyId()), request.name());
        Category saved = categoryRepository.save(category);
        return ResponseEntity
                .ok(ApiResponse.success("Category created successfully", new CategoryResponse(saved.id(),
                        saved.parentId(), saved.name(), saved.color(), saved.isActive())));
    }

    // ==================== PRODUCT ENDPOINTS ====================

    @GetMapping("/products")
    @Operation(summary = "Search products", description = "Searches products by filters with pagination")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @Parameter(description = "Company identifier") @RequestParam UUID companyId,
            @Parameter(description = "Product name filter") @RequestParam(required = false) String name,
            @Parameter(description = "Product reference filter") @RequestParam(required = false) String reference,
            @Parameter(description = "Category identifier filter") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        List<Product> products = jdbcProductPersistenceAdapter.searchProducts(
                new CompanyId(companyId), name, reference, categoryId, page, size);

        List<ProductResponse> response = products.stream()
                .map(p -> new ProductResponse(
                        p.id().value(),
                        p.reference(),
                        p.name(),
                        p.shortName(),
                        p.categoryId(),
                        p.taxId(),
                        p.type().name(),
                        p.isActive()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Products fetched successfully", response));
    }

    @PostMapping("/products")
    @Operation(summary = "Create product", description = "Creates a new product in catalog")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        CompanyId companyId = new CompanyId(request.companyId());

        if (productRepository.existsByCompanyAndReference(companyId, request.reference())) {
            throw new RuntimeException("Product reference already exists");
        }

        Product product = Product.create(
                companyId,
                request.reference(),
                request.name(),
                request.taxId(),
                ProductType.valueOf(request.type()));

        Product saved = productRepository.save(product);

        return ResponseEntity.ok(ApiResponse.success("Product created successfully", new ProductResponse(
                saved.id().value(),
                saved.reference(),
                saved.name(),
                saved.shortName(),
                saved.categoryId(),
                saved.taxId(),
                saved.type().name(),
                saved.isActive())));
    }

    @PutMapping("/products/{id}")
    @Operation(summary = "Update product", description = "Updates basic product information")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @Parameter(description = "Product identifier") @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {

        Product product = productRepository.findById(new ProductId(id))
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.updateBasicInfo(
                request.name(),
                request.shortName(),
                request.description(),
                request.categoryId());

        Product saved = productRepository.save(product);

        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", new ProductResponse(
                saved.id().value(),
                saved.reference(),
                saved.name(),
                saved.shortName(),
                saved.categoryId(),
                saved.taxId(),
                saved.type().name(),
                saved.isActive())));
    }

    @GetMapping("/products/{id}/prices")
    @Operation(summary = "Get product prices", description = "Returns configured prices for a product across tariffs")
    public ResponseEntity<ApiResponse<List<ProductPriceResponse>>> getProductPrices(
            @Parameter(description = "Product identifier") @PathVariable Long id,
            @RequestParam UUID companyId) {
        // Get all tariffs and check prices for each
        List<Tariff> tariffs = tariffRepository.findByCompanyId(new CompanyId(companyId));
        List<ProductPrice> prices = tariffs.stream()
                .map(t -> productPriceRepository.findByProductAndTariff(new ProductId(id), t.id()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        List<ProductPriceResponse> response = prices.stream()
                .map(p -> new ProductPriceResponse(p.tariffId(), p.price().cents()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Product prices fetched successfully", response));
    }

    @PutMapping("/products/{id}/prices")
    @Operation(summary = "Update product prices", description = "Creates or updates prices for product tariffs")
    public ResponseEntity<ApiResponse<Void>> updateProductPrices(
            @Parameter(description = "Product identifier") @PathVariable Long id,
            @Valid @RequestBody UpdateProductPricesRequest request) {

        ProductId productId = new ProductId(id);

        for (UpdateProductPricesRequest.TariffPrice tp : request.prices()) {
            ProductPrice price = ProductPrice.create(
                    productId,
                    tp.tariffId(),
                    Money.ofEurosCents(tp.priceCents()));
            productPriceRepository.save(price);
        }

        return ResponseEntity.ok(ApiResponse.success("Product prices updated successfully"));
    }

    // ==================== TARIFF ENDPOINTS ====================

    @GetMapping("/tariffs")
    @Operation(summary = "List tariffs", description = "Returns tariffs configured for a company")
    public ResponseEntity<ApiResponse<List<TariffResponse>>> listTariffs(@RequestParam UUID companyId) {
        List<Tariff> tariffs = tariffRepository.findByCompanyId(new CompanyId(companyId));
        List<TariffResponse> response = tariffs.stream()
                .map(t -> new TariffResponse(t.id(), t.name(), t.priceType(), t.isDefault()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Tariffs fetched successfully", response));
    }

    // ==================== RECORDS ====================

    public record TaxResponse(Long id, String name, BigDecimal rate, BigDecimal surcharge) {
    }

    public record CategoryResponse(Long id, Long parentId, String name, String color, boolean active) {
    }

    public record CreateCategoryRequest(UUID companyId, String name) {
    }

    public record ProductResponse(Long id, String reference, String name, String shortName,
            Long categoryId, Long taxId, String type, boolean active) {
    }

    public record CreateProductRequest(UUID companyId, String reference, String name, String shortName,
            Long taxId, String type) {
    }

    public record UpdateProductRequest(String name, String shortName, String description, Long categoryId) {
    }

    public record ProductPriceResponse(Long tariffId, long priceCents) {
    }

    public record UpdateProductPricesRequest(List<TariffPrice> prices) {
        public record TariffPrice(Long tariffId, long priceCents) {
        }
    }

    public record TariffResponse(Long id, String name, String priceType, boolean isDefault) {
    }
}
