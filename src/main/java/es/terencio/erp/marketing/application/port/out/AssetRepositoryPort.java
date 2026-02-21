package es.terencio.erp.marketing.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import es.terencio.erp.marketing.domain.model.CompanyAsset;

public interface AssetRepositoryPort {
    CompanyAsset save(CompanyAsset asset);
    Optional<CompanyAsset> findByIdAndCompanyId(UUID id, UUID companyId);
    void deleteById(UUID id);
    long countByFilters(UUID companyId, String search, String contentType);
    List<CompanyAsset> findByFiltersPaginated(UUID companyId, String search, String contentType, int offset, int limit);
}