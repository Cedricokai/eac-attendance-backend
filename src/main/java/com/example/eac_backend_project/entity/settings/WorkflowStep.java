package com.example.eac_backend_project.entity.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "inventory_workflow_steps")
public class WorkflowStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "workflow_definition_id", nullable = false)
    @JsonIgnore   // <-- stops recursion
    private WorkflowDefinition workflowDefinition;

    private Integer stepOrder;
    private String stepName;
    private String requiredRole;
    private String targetStatus;
    private String rejectStatus;
    private boolean requiresApproval = true;
    private boolean isFinalStep = false;

    // ========== Getters and Setters ==========
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WorkflowDefinition getWorkflowDefinition() { return workflowDefinition; }
    public void setWorkflowDefinition(WorkflowDefinition workflowDefinition) { this.workflowDefinition = workflowDefinition; }

    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public String getRequiredRole() { return requiredRole; }
    public void setRequiredRole(String requiredRole) { this.requiredRole = requiredRole; }

    public String getTargetStatus() { return targetStatus; }
    public void setTargetStatus(String targetStatus) { this.targetStatus = targetStatus; }

    public String getRejectStatus() { return rejectStatus; }
    public void setRejectStatus(String rejectStatus) { this.rejectStatus = rejectStatus; }

    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }

    public boolean isFinalStep() { return isFinalStep; }
    public void setFinalStep(boolean finalStep) { isFinalStep = finalStep; }
}