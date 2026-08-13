package com.example.eac_backend_project.Service.settings;

import com.example.eac_backend_project.entity.settings.WorkflowCondition;
import com.example.eac_backend_project.entity.settings.WorkflowDefinition;
import com.example.eac_backend_project.entity.settings.WorkflowStep;
import com.example.eac_backend_project.repository.settings.WorkflowDefinitionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Service
public class WorkflowService {

    @Autowired
    private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // ─── Default workflow initialisation ───

    @PostConstruct
    public void initDefaultWorkflows() {
        createDefaultWorkflowIfMissing("INVENTORY");
        createDefaultWorkflowIfMissing("DIRECT_PURCHASE");
    }

    private void createDefaultWorkflowIfMissing(String entityType) {

        String defaultName = "Default " + entityType + " Workflow";

        // Check by name (unique column) – if exists, skip creation
        if (workflowDefinitionRepository.findByName(defaultName).isPresent()) {
            System.out.println("Default workflow already exists for: " + entityType);
            return;
        }
        // Check if a default (no‑condition) workflow already exists for this entity type
        boolean exists = workflowDefinitionRepository
                .findFirstByActiveTrueAndEntityTypeAndConditionJsonIsNull(entityType)
                .isPresent();
        if (exists) return;

        WorkflowDefinition defaultWf = new WorkflowDefinition();
        defaultWf.setName("Default " + entityType + " Workflow");
        defaultWf.setDescription("Automatically created default workflow for " + entityType);
        defaultWf.setActive(true);
        defaultWf.setEntityType(entityType);
        defaultWf.setConditionJson(null);   // no conditions – matches all requests

        List<WorkflowStep> steps = new ArrayList<>();

        if ("INVENTORY".equals(entityType)) {
            // Inventory default: Planner → Procurement → Store → Issue
            steps.add(createStep(0, "Planner Approval", "PLANNER", "APPROVED_BY_PLANNER", "REJECTED_BY_PLANNER", true, false));
            steps.add(createStep(1, "Procurement Approval", "PROCUREMENT", "PROCURED", "REJECTED_BY_PROCUREMENT", true, false));
            steps.add(createStep(2, "Store Review", "STORE", "STORE_REVIEW", "REJECTED_BY_STORE", true, false));
            steps.add(createStep(3, "Issue Items", "STORE", "ISSUED", null, true, true));
        } else if ("DIRECT_PURCHASE".equals(entityType)) {
            // Direct Purchase default: Accountant → Reviewer → Paid
            steps.add(createStep(0, "Accountant Approval", "ACCOUNTANT", "ACCOUNTANT_APPROVED", "REJECTED", true, false));
            steps.add(createStep(1, "Reviewer Approval", "REVIEWER", "REVIEWER_APPROVED", "REJECTED", true, false));
            steps.add(createStep(2, "Payment", "FINANCE", "PAID", null, true, true));
        }

        // ✅ CRITICAL: associate each step with the workflow
        for (WorkflowStep step : steps) {
            step.setWorkflowDefinition(defaultWf);
        }
        defaultWf.setSteps(steps);

        workflowDefinitionRepository.save(defaultWf);
        System.out.println("✅ Created default workflow for entity type: " + entityType);
    }

    private WorkflowStep createStep(int order, String name, String role, String targetStatus,
                                    String rejectStatus, boolean requiresApproval, boolean isFinal) {
        WorkflowStep step = new WorkflowStep();
        step.setStepOrder(order);
        step.setStepName(name);
        step.setRequiredRole(role);
        step.setTargetStatus(targetStatus);
        step.setRejectStatus(rejectStatus);
        step.setRequiresApproval(requiresApproval);
        step.setFinalStep(isFinal);
        return step;
    }

    // ─── Core workflow methods ───

    public WorkflowDefinition findMatchingWorkflow(String entityType, Object entity) {
        List<WorkflowDefinition> activeWorkflows = workflowDefinitionRepository
                .findByActiveTrueAndEntityType(entityType);

        for (WorkflowDefinition wf : activeWorkflows) {
            if (matchesConditions(entity, wf.getConditionJson())) {
                return wf;
            }
        }

        // Fallback to default (no‑condition) workflow
        return workflowDefinitionRepository
                .findFirstByActiveTrueAndEntityTypeAndConditionJsonIsNull(entityType)
                .orElseThrow(() -> new RuntimeException(
                        "No default workflow defined for entity type: " + entityType));
    }

    private boolean matchesConditions(Object entity, String conditionJson) {
        if (conditionJson == null || conditionJson.trim().isEmpty()) return true;
        try {
            List<WorkflowCondition> conditions = objectMapper.readValue(conditionJson,
                    new TypeReference<List<WorkflowCondition>>() {});
            for (WorkflowCondition cond : conditions) {
                if (!evaluateCondition(entity, cond)) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean evaluateCondition(Object entity, WorkflowCondition cond) {
        Object fieldValue = getFieldValue(entity, cond.getField());
        Object condValue = cond.getValue();
        String op = cond.getOperator();

        if (fieldValue == null) return false;

        switch (op) {
            case "eq": return fieldValue.equals(condValue);
            case "ne": return !fieldValue.equals(condValue);
            case "gt":
                if (fieldValue instanceof Number && condValue instanceof Number) {
                    return ((Number) fieldValue).doubleValue() > ((Number) condValue).doubleValue();
                }
                return false;
            case "lt":
                if (fieldValue instanceof Number && condValue instanceof Number) {
                    return ((Number) fieldValue).doubleValue() < ((Number) condValue).doubleValue();
                }
                return false;
            case "contains":
                if (fieldValue instanceof String && condValue instanceof String) {
                    return ((String) fieldValue).toLowerCase().contains(((String) condValue).toLowerCase());
                }
                return false;
            default: return false;
        }
    }

    // In WorkflowService.java
    public WorkflowDefinition saveWorkflow(WorkflowDefinition workflow) {
        // Validate each step has required fields
        for (WorkflowStep step : workflow.getSteps()) {
            if (step.getTargetStatus() == null || step.getTargetStatus().trim().isEmpty()) {
                throw new IllegalArgumentException("Step " + step.getStepName() + " is missing target status");
            }

            // Validate requiredRole format (allow comma-separated)
            if (step.getRequiredRole() != null && !step.getRequiredRole().trim().isEmpty()) {
                String[] roles = step.getRequiredRole().split(",");
                for (String role : roles) {
                    if (role.trim().isEmpty()) {
                        throw new IllegalArgumentException("Invalid role format in step: " + step.getStepName());
                    }
                }
            }
        }
        return workflowDefinitionRepository.save(workflow);
    }

    private Object getFieldValue(Object entity, String field) {
        try {
            String getterName = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
            Method method = entity.getClass().getMethod(getterName);
            return method.invoke(entity);
        } catch (NoSuchMethodException e) {
            try {
                String isGetterName = "is" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
                Method method = entity.getClass().getMethod(isGetterName);
                return method.invoke(entity);
            } catch (Exception ex) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public WorkflowStep getNextStep(WorkflowDefinition workflow, String currentStatus) {
        List<WorkflowStep> steps = workflow.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            WorkflowStep step = steps.get(i);
            if (step.getTargetStatus().equals(currentStatus)) {
                if (step.isFinalStep()) return null;
                return (i + 1 < steps.size()) ? steps.get(i + 1) : null;
            }
        }
        return null;
    }

    public WorkflowStep getStepByStatus(WorkflowDefinition workflow, String status) {
        return workflow.getSteps().stream()
                .filter(step -> step.getTargetStatus().equals(status))
                .findFirst()
                .orElse(null);
    }
}