package com.example.eac_backend_project.dto;

import lombok.Data;
import java.util.Map;

@Data
public class GeneratePayrollRequest {
    private String rateChoice;
    private Long periodId;
    private java.util.List<Long> excludedEmployeeIds;
    private Long employeeId; // null means all eligible employees
    private Map<Long, Map<String, Double>> temporaryAllowances;
    private SettingsDTO settings;
    private Map<Long, Integer> leaveDays;
    private Boolean includeBasicSalary = true;
    private Map<Long, Double> creditAmounts;
}
