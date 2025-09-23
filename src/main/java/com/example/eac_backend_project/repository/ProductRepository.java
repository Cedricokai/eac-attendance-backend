package com.example.eac_backend_project.repository;


import com.example.eac_backend_project.entity.inventory.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code);
    // In ProductRepository
    void deleteById(Long id);

    Optional<Product> findById(long id);
}