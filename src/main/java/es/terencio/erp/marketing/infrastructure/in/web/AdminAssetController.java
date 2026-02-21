package es.terencio.erp.marketing.infrastructure.in.web;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.auth.domain.model.Permission;
import es.terencio.erp.auth.infrastructure.config.security.aop.RequiresPermission;
import es.terencio.erp.marketing.application.dto.AssetResponse;
import es.terencio.erp.marketing.application.port.in.ManageAssetsUseCase;
import es.terencio.erp.shared.domain.query.PageResult;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/marketing/assets")
@Tag(name = "Marketing Assets", description = "File uploads and asset management")
public class AdminAssetController {

    private final ManageAssetsUseCase manageAssetsUseCase;

    public AdminAssetController(ManageAssetsUseCase manageAssetsUseCase) {
        this.manageAssetsUseCase = manageAssetsUseCase;
    }

    @GetMapping
    @Operation(summary = "Search and list paginated assets")
    @RequiresPermission(permission = Permission.MARKETING_TEMPLATE_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<PageResult<AssetResponse>>> listAssets(
            @PathVariable UUID companyId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String contentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResult<AssetResponse> result = manageAssetsUseCase.searchAssets(companyId, search, contentType, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{assetId}")
    @Operation(summary = "Get asset metadata by ID")
    @RequiresPermission(permission = Permission.MARKETING_TEMPLATE_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<AssetResponse>> getAsset(@PathVariable UUID companyId, @PathVariable UUID assetId) {
        return ResponseEntity.ok(ApiResponse.success(manageAssetsUseCase.getAsset(companyId, assetId)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a new file/asset")
    @RequiresPermission(permission = Permission.MARKETING_TEMPLATE_EDIT, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<AssetResponse>> uploadAsset(
            @PathVariable UUID companyId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "true") boolean isPublic) throws Exception {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("File is empty or missing"));
        }
        
        AssetResponse uploaded = manageAssetsUseCase.uploadAsset(
                companyId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream(),
                isPublic);
        return ResponseEntity.ok(ApiResponse.success("Asset uploaded successfully", uploaded));
    }

    @DeleteMapping("/{assetId}")
    @Operation(summary = "Delete an asset")
    @RequiresPermission(permission = Permission.MARKETING_TEMPLATE_DELETE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> deleteAsset(@PathVariable UUID companyId, @PathVariable UUID assetId) {
        manageAssetsUseCase.deleteAsset(companyId, assetId);
        return ResponseEntity.ok(ApiResponse.success("Asset deleted successfully"));
    }
}