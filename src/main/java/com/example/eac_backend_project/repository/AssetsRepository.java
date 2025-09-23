package com.example.eac_backend_project.repository;


import com.example.eac_backend_project.config.Assets;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetsRepository extends JpaRepository<Assets, Long> {
    List<Assets> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code);
    // In ProductRepository
    void deleteById(Long id);

    Optional<Assets> findById(long id);
   
}
