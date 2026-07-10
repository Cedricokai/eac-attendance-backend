package com.example.eac_backend_project.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PayrollPeriodDTO {
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String category;
    private String status;
    private Integer employeeCount; // Optional: count of employees in this category
    private Boolean hasGeneratedPayroll; // Optional: whether payroll has been generated
    private boolean convertExcessToOvertime = true;

    public PayrollPeriodDTO() {}

    public PayrollPeriodDTO(Long id, String name, LocalDate startDate, LocalDate endDate,
                            String category, String status) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.category = category;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(Integer employeeCount) {
        this.employeeCount = employeeCount;
    }

    public Boolean getHasGeneratedPayroll() {
        return hasGeneratedPayroll;
    }

    public void setHasGeneratedPayroll(Boolean hasGeneratedPayroll) {
        this.hasGeneratedPayroll = hasGeneratedPayroll;
    }
}