package com.example.eac_backend_project.dto;

public class LoanReversalRequestDTO {
    private Long payrollPeriodId;
    private Long employeeId;  // Optional - if null, reverse for all employees
    private String reason;
    private String reversedBy;
    private boolean restoreOriginalNetSalary = true;

    // Getters and Setters
    public Long getPayrollPeriodId() {
        return payrollPeriodId;
    }

    public void setPayrollPeriodId(Long payrollPeriodId) {
        this.payrollPeriodId = payrollPeriodId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReversedBy() {
        return reversedBy;
    }

    public void setReversedBy(String reversedBy) {
        this.reversedBy = reversedBy;
    }

    public boolean isRestoreOriginalNetSalary() {
        return restoreOriginalNetSalary;
    }

    public void setRestoreOriginalNetSalary(boolean restoreOriginalNetSalary) {
        this.restoreOriginalNetSalary = restoreOriginalNetSalary;
    }
}