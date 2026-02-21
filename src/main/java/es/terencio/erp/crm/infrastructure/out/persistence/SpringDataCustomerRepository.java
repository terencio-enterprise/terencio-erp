package es.terencio.erp.crm.infrastructure.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataCustomerRepository extends 
        JpaRepository<CustomerJpaEntity, Long>, 
        JpaSpecificationExecutor<CustomerJpaEntity> {

    Optional<CustomerJpaEntity> findByUuidAndCompanyIdAndDeletedAtIsNull(UUID uuid, UUID companyId);

    boolean existsByEmailIgnoreCaseAndCompanyIdAndDeletedAtIsNull(String email, UUID companyId);
}