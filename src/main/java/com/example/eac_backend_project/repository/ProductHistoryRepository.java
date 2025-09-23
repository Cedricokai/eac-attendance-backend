package com.example.eac_backend_project.repository;


import java.util.Optional;

import com.example.eac_backend_project.entity.inventory.ProductHistory;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ProductHistoryRepository extends JpaRepository<ProductHistory, Long> {
    Optional<ProductHistory> findByProductId(Long productId);
    Optional<ProductHistory> findByAssetsId(Long assetsId);
}
