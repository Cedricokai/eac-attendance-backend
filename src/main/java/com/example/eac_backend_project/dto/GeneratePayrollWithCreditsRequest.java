package com.example.eac_backend_project.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePayrollWithCreditsRequest {
    private String rateChoice;
    private Long periodId;
    private java.util.List<Long> excludedEmployeeIds;
    private Long employeeId; // null means all eligible employees
    private List<Long> selectedCreditIds;  // IDs of credits to apply
    private Map<Long, Map<String, Double>> temporaryAllowances;
    private SettingsDTO settings;
    private Boolean includeBasicSalary = true;
    private Map<Long, Double> directCreditAmounts;
}
