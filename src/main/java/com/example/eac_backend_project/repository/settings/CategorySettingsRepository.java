package com.example.eac_backend_project.repository.settings;

import com.example.eac_backend_project.entity.settings.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategorySettingsRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    boolean existsByName(String name);
    List<Category> findAllByOrderByNameAsc();
    List<Category> findBySkipAutoAbsentTrue();
}