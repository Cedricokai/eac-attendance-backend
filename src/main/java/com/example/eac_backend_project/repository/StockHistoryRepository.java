package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.inventory.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {

    List<StockHistory> findByProductIdOrderByAdjustmentDateDesc(Long productId);

    List<StockHistory> findByProductNameContainingIgnoreCase(String productName);

    List<StockHistory> findByUserEmail(String userEmail);

    @Query("SELECT sh FROM StockHistory sh WHERE sh.productId = :productId AND sh.adjustmentDate BETWEEN :startDate AND :endDate ORDER BY sh.adjustmentDate DESC")
    List<StockHistory> findByProductIdAndDateRange(
            @Param("productId") Long productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT sh FROM StockHistory sh WHERE sh.adjustmentType = :adjustmentType ORDER BY sh.adjustmentDate DESC")
    List<StockHistory> findByAdjustmentType(@Param("adjustmentType") String adjustmentType);
}