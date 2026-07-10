package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.inventory.CostCenterProduct;
import com.example.eac_backend_project.entity.inventory.CostCenterProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CostCenterProductRepository extends JpaRepository<CostCenterProduct, Long> {

    List<CostCenterProduct> findByCostCenterId(Long costCenterId);

    List<CostCenterProduct> findByProductId(Long productId);

    List<CostCenterProduct> findByStatus(CostCenterProductStatus status);

    List<CostCenterProduct> findByCostCenterIdAndStatus(Long costCenterId, CostCenterProductStatus status);

    @Query("SELECT ccp FROM CostCenterProduct ccp WHERE ccp.costCenter.id = :costCenterId " +
            "AND ccp.product.id = :productId")
    Optional<CostCenterProduct> findByCostCenterAndProduct(
            @Param("costCenterId") Long costCenterId,
            @Param("productId") Long productId);

    @Query("SELECT SUM(ccp.totalCost) FROM CostCenterProduct ccp " +
            "WHERE ccp.costCenter.id = :costCenterId")
    Double getTotalProductCostForCostCenter(@Param("costCenterId") Long costCenterId);

    @Query("SELECT p FROM CostCenterProduct p WHERE p.costCenter.id = :costCenterId " +
            "AND (p.status = 'PENDING' OR p.status = 'APPROVED')")
    List<CostCenterProduct> findPendingAndApprovedProducts(@Param("costCenterId") Long costCenterId);

    CostCenterProduct findByCostCenterIdAndProductId(Long costCenterId, Long productId);

    @Query("SELECT ccp FROM CostCenterProduct ccp WHERE ccp.costCenter.job.id = :jobId")
    List<CostCenterProduct> findByCostCenterJobId(Long jobId);

    List<CostCenterProduct> findByInventoryRequestId(Long requestId);

}