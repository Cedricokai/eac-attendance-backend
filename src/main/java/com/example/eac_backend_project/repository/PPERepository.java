package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.PPE;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PPERepository extends JpaRepository<PPE, Long> {

    Optional<PPE> findByCode(String code);

    List<PPE> findByNameContainingIgnoreCase(String name);

    List<PPE> findByPpeTypeContainingIgnoreCase(String ppeType);

    List<PPE> findByBrandContainingIgnoreCase(String brand);

    List<PPE> findByLocationContainingIgnoreCase(String location);

    List<PPE> findByIsActiveTrue();

    List<PPE> findByStockLessThan(Integer stock);

    List<PPE> findByStockLessThanEqualAndMinStockLevelGreaterThan(Integer stock, Integer minStockLevel);

    @Query("SELECT p FROM PPE p WHERE p.name LIKE %:query% OR p.code LIKE %:query% OR p.description LIKE %:query% OR p.brand LIKE %:query%")
    List<PPE> searchPPE(String query);

    @Query("SELECT COUNT(p) FROM PPE p WHERE p.stock <= p.minStockLevel AND p.isActive = true")
    Long countLowStockItems();

    @Query("SELECT SUM(p.stock * p.cost) FROM PPE p WHERE p.cost IS NOT NULL")
    Double calculateTotalInventoryValue();
}