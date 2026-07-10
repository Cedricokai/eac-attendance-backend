package com.example.eac_backend_project.dto;

import lombok.Data;

@Data
public class CategoryDTO {
    private Long id;
    private String name;
    private Integer standardRateHours;
    private Boolean useNonTaxableAllowances;
    private String workStartTime;
    private Boolean excludeWeekendsFromLeave;
    private Integer annualLeaveDays;
    private Boolean skipAutoAbsent;
    private Boolean applyWithholdingTax;

    // NEW FIELDS
    private String supervisorEmails;
    private String plannerEmails;
    private String hrEmails;
    private Boolean useCustomApprovers;

    public CategoryDTO() {}

    public CategoryDTO(Long id, String name, Integer standardRateHours,
                       Boolean useNonTaxableAllowances, String workStartTime,
                       Boolean excludeWeekendsFromLeave) {
        this.id = id;
        this.name = name;
        this.standardRateHours = standardRateHours;
        this.useNonTaxableAllowances = useNonTaxableAllowances;
        this.workStartTime = workStartTime;
        this.excludeWeekendsFromLeave = excludeWeekendsFromLeave;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getStandardRateHours() { return standardRateHours; }
    public void setStandardRateHours(Integer standardRateHours) { this.standardRateHours = standardRateHours; }

    public Boolean getUseNonTaxableAllowances() { return useNonTaxableAllowances; }
    public void setUseNonTaxableAllowances(Boolean useNonTaxableAllowances) { this.useNonTaxableAllowances = useNonTaxableAllowances; }

    public String getWorkStartTime() { return workStartTime; }
    public void setWorkStartTime(String workStartTime) { this.workStartTime = workStartTime; }

    public Boolean getExcludeWeekendsFromLeave() { return excludeWeekendsFromLeave; }
    public void setExcludeWeekendsFromLeave(Boolean excludeWeekendsFromLeave) { this.excludeWeekendsFromLeave = excludeWeekendsFromLeave; }
}