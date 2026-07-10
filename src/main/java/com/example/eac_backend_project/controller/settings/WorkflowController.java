package com.example.eac_backend_project.controller.settings;

import com.example.eac_backend_project.entity.settings.WorkflowDefinition;
import com.example.eac_backend_project.entity.settings.WorkflowStep;
import com.example.eac_backend_project.repository.settings.WorkflowDefinitionRepository;
import com.example.eac_backend_project.repository.settings.WorkflowStepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settings/workflows")
public class WorkflowController {

    @Autowired
    private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Autowired
    private WorkflowStepRepository workflowStepRepository;

    @GetMapping
    public List<WorkflowDefinition> getAllWorkflows() {
        return workflowDefinitionRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowDefinition> getWorkflow(@PathVariable Long id) {
        return workflowDefinitionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public WorkflowDefinition createWorkflow(@RequestBody WorkflowDefinition definition) {
        // Ensure steps are linked
        if (definition.getSteps() != null) {
            definition.getSteps().forEach(step -> step.setWorkflowDefinition(definition));
        }
        return workflowDefinitionRepository.save(definition);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowDefinition> updateWorkflow(@PathVariable Long id, @RequestBody WorkflowDefinition definition) {
        if (!workflowDefinitionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        definition.setId(id);
        if (definition.getSteps() != null) {
            definition.getSteps().forEach(step -> step.setWorkflowDefinition(definition));
        }
        return ResponseEntity.ok(workflowDefinitionRepository.save(definition));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable Long id) {
        workflowDefinitionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{workflowId}/steps")
    public WorkflowStep addStep(@PathVariable Long workflowId, @RequestBody WorkflowStep step) {
        WorkflowDefinition workflow = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));
        step.setWorkflowDefinition(workflow);
        return workflowStepRepository.save(step);
    }

    @PutMapping("/steps/{stepId}")
    public WorkflowStep updateStep(@PathVariable Long stepId, @RequestBody WorkflowStep step) {
        step.setId(stepId);
        return workflowStepRepository.save(step);
    }

    @DeleteMapping("/steps/{stepId}")
    public ResponseEntity<Void> deleteStep(@PathVariable Long stepId) {
        workflowStepRepository.deleteById(stepId);
        return ResponseEntity.noContent().build();
    }
}