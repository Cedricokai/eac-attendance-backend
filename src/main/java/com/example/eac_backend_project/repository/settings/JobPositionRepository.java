package com.example.eac_backend_project.repository.settings;

import com.example.eac_backend_project.entity.settings.JobPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobPositionRepository extends JpaRepository<JobPosition, Long> {

    Optional<JobPosition> findByName(String name);

    // Remove this method as it's causing the error
    // List<JobPosition> findByCategory(String category);

    // Find by category ID (for the new Category entity relationship)
    @Query("SELECT jp FROM JobPosition jp WHERE jp.category.id = :categoryId")
    List<JobPosition> findByCategoryId(@Param("categoryId") Long categoryId);

    // Find by category name through the Category entity
    @Query("SELECT jp FROM JobPosition jp WHERE jp.category.name = :categoryName")
    List<JobPosition> findByCategoryName(@Param("categoryName") String categoryName);

    List<JobPosition> findAllByOrderByNameAsc();
    List<JobPosition> findByDepartmentId(Long departmentId);
    boolean existsByName(String name);
}