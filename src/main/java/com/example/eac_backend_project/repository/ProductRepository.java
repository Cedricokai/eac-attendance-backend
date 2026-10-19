package com.example.eac_backend_project.repository;


import com.example.eac_backend_project.entity.inventory.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Product e where e.id = :id")
    java.util.Optional<Product> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    List<Product> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code);

    void deleteById(Long id);

    Optional<Product> findById(long id);

    // ✅ ADD: Custom method for direct stock update
    @Modifying
    @Query("UPDATE Product p SET p.stock = :newStock, p.lastUpdatedDate = CURRENT_TIMESTAMP WHERE p.id = :productId")
    void updateStock(@Param("productId") Long productId, @Param("newStock") int newStock);
}