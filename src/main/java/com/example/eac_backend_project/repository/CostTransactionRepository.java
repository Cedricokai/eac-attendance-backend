package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.inventory.CostCenter;
import com.example.eac_backend_project.entity.inventory.CostTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CostTransactionRepository extends JpaRepository<CostTransaction, Long> {
    List<CostTransaction> findByCostCenterId(Long costCenterId);
    List<CostTransaction> findByJobId(Long jobId);
    List<CostTransaction> findByProductId(Long productId);

    @Query("SELECT SUM(ct.amount) FROM CostTransaction ct WHERE ct.costCenter.id = :costCenterId")
    Double getTotalSpentByCostCenter(@Param("costCenterId") Long costCenterId);

    @Query("SELECT ct FROM CostTransaction ct WHERE ct.transactionDate BETWEEN :startDate AND :endDate")
    List<CostTransaction> findTransactionsBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );;

    Optional<CostTransaction> findByTransactionNumber(String transactionNumber);

    List<CostTransaction> findByIssuedById(Long userId);

    void deleteByCostCenter(CostCenter costCenter);
}