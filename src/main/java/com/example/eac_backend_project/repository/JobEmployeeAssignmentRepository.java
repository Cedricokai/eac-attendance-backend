package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.JobEmployeeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobEmployeeAssignmentRepository extends JpaRepository<JobEmployeeAssignment, Long> {

    List<JobEmployeeAssignment> findByJobId(Long jobId);

    Optional<JobEmployeeAssignment> findByJobIdAndEmployeeId(Long jobId, Long employeeId);

    @Modifying
    @Query("DELETE FROM JobEmployeeAssignment a WHERE a.job.id = :jobId AND a.employee.id = :employeeId")
    void deleteByJobIdAndEmployeeId(@Param("jobId") Long jobId, @Param("employeeId") Long employeeId);

    @Modifying
    @Query("DELETE FROM JobEmployeeAssignment a WHERE a.job.id = :jobId")
    void deleteAllByJobId(@Param("jobId") Long jobId);
}