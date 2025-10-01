package com.example.eac_backend_project.dto;

import java.util.Map;

public class GeneratePayrollRequest {
    private Long periodId;
    private Map<Long, Map<String, Double>> temporaryAllowances;
    // Key: employeeId, Value: allowances by type (housingAllowance, tntAllowance, clothsAllowances, otherAllowances)

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    public Map<Long, Map<String, Double>> getTemporaryAllowances() { return temporaryAllowances; }
    public void setTemporaryAllowances(Map<Long, Map<String, Double>> temporaryAllowances) {
        this.temporaryAllowances = temporaryAllowances;
    }
}
