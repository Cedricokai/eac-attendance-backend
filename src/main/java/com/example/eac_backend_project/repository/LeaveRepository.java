package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.Leave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {
    List<Leave> findByEmployeeId(Long employeeId);
    List<Leave> findByStatus(String status);
    List<Leave> findBySupervisorStatus(String status);
    List<Leave> findByPlannerStatus(String status);
    List<Leave> findByHrStatus(String status);

}