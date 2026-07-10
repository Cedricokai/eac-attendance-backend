package com.example.eac_backend_project.entity.settings;

public class WorkflowCondition {
    private String field;    // e.g., "ppeRequest", "hasManualProducts", "department"
    private String operator; // eq, ne, gt, lt, contains
    private Object value;

    public WorkflowCondition() {}

    public WorkflowCondition(String field, String operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    // Getters and setters
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
}