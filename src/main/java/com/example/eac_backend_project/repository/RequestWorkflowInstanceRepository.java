package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.RequestWorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RequestWorkflowInstanceRepository extends JpaRepository<RequestWorkflowInstance, Long> {
    Optional<RequestWorkflowInstance> findByInventoryRequestId(Long requestId);
}
