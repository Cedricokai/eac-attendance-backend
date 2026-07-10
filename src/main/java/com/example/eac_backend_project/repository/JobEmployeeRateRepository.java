package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.JobEmployeeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobEmployeeRateRepository extends JpaRepository<JobEmployeeRate, Long> {
    Optional<JobEmployeeRate> findByJobIdAndEmployeeId(Long jobId, Long employeeId);
    List<JobEmployeeRate> findByJobId(Long jobId);
    void deleteByJobId(Long jobId);

    @Modifying
    @Query("DELETE FROM JobEmployeeRate r WHERE r.job.id = :jobId AND r.employee.id = :employeeId")
    void deleteByJobIdAndEmployeeId(@Param("jobId") Long jobId, @Param("employeeId") Long employeeId);
}