package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.Comparison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ComparisonRepository extends JpaRepository<Comparison, Long> {
    List<Comparison> findByCreatedByOrderByCreatedAtDesc(String createdBy);
}