package com.example.eac_backend_project.repository.settings;

import com.example.eac_backend_project.entity.settings.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {
    List<WorkflowDefinition> findByActiveTrue();
    Optional<WorkflowDefinition> findFirstByActiveTrueAndConditionJsonIsNull(); // default workflow
    List<WorkflowDefinition> findByActiveTrueAndEntityType(String entityType);
    Optional<WorkflowDefinition> findFirstByActiveTrueAndEntityTypeAndConditionJsonIsNull(String entityType);
}