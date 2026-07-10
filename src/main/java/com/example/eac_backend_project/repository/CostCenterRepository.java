package com.example.eac_backend_project.repository;


import com.example.eac_backend_project.entity.inventory.CostCenter;
import com.example.eac_backend_project.entity.inventory.CostCenterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CostCenterRepository extends JpaRepository<CostCenter, Long> {
    Optional<CostCenter> findByCode(String code);
    Optional<CostCenter> findByJob_Id(Long jobId);
    List<CostCenter> findByStatus(CostCenterStatus status);

    @Query("SELECT cc FROM CostCenter cc WHERE cc.budget - cc.spentAmount < :threshold")
    List<CostCenter> findLowBudgetCenters(@Param("threshold") Double threshold);

    @Query("SELECT cc FROM CostCenter cc WHERE cc.spentAmount / cc.budget * 100 >= :percentage")
    List<CostCenter> findHighUtilizationCenters(@Param("percentage") Double percentage);
}