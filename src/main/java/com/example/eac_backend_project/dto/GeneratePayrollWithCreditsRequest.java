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
    private Long periodId;
    private List<Long> selectedCreditIds;  // IDs of credits to apply
    private Map<Long, Map<String, Double>> temporaryAllowances;
    private SettingsDTO settings;
    private Boolean includeBasicSalary;
    private Map<Long, Double> directCreditAmounts;
}