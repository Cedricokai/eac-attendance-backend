package com.example.eac_backend_project.repository.settings;

import com.example.eac_backend_project.entity.settings.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {
}