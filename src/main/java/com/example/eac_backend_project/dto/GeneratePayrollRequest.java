package com.example.eac_backend_project.dto;

import lombok.Data;
import java.util.Map;

@Data
public class GeneratePayrollRequest {
    private Long periodId;
    private Map<Long, Map<String, Double>> temporaryAllowances;
    private SettingsDTO settings;
    private Map<Long, Integer> leaveDays;
    private Boolean includeBasicSalary;
    private Map<Long, Double> creditAmounts;
}
