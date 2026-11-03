package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.inventory.ItAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItAssetRepository extends JpaRepository<ItAsset, Long> {
    List<ItAsset> findAllByOrderByCreatedAtDesc();
    List<ItAsset> findByEmployeeIdOrderByTakenAtDesc(Long employeeId);
    List<ItAsset> findByIsPublicTrueOrderByCreatedAtDesc();
    List<ItAsset> findByReturnedFalseAndIsPublicFalseOrderByTakenAtDesc();
}

