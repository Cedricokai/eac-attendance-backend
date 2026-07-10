package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.JobProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// JobProductRepository.java
@Repository
public interface JobProductRepository extends JpaRepository<JobProduct, Long> {
    List<JobProduct> findByJobId(Long jobId);
    Optional<JobProduct> findByJobIdAndProductId(Long jobId, Long productId);
    @Modifying
    @Query("DELETE FROM JobProduct jp WHERE jp.job.id = :jobId")
    void deleteByJobId(@Param("jobId") Long jobId);
}